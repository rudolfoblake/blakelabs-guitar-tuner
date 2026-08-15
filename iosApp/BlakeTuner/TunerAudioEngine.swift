import AVFoundation
import BlakeTunerShared

final class TunerAudioEngine {
    struct Settings {
        var mode = 0
        var preset = 0
        var selectedStringIndex = -1
        var a4Hz = 440.0
    }

    var onSignal: ((Float) -> Void)?
    var onFrame: ((TunerFrame) -> Void)?
    var onError: ((String) -> Void)?

    private let engine = AVAudioEngine()
    private let analysisQueue = DispatchQueue(label: "com.blakelabs.guitartuner.analysis", qos: .userInitiated)
    private var rollingSamples = [Int16]()
    private var processor: TunerProcessor?
    private var settings = Settings()
    private var running = false

    func update(settings: Settings) {
        analysisQueue.async { [weak self] in
            guard let self else { return }
            self.settings = settings
            self.processor?.reset()
            self.rollingSamples.removeAll(keepingCapacity: true)
        }
    }

    func start() {
        guard !running else { return }
        requestPermission { [weak self] granted in
            guard let self else { return }
            guard granted else {
                DispatchQueue.main.async {
                    self.onError?("Microphone permission is required to tune.")
                }
                return
            }
            DispatchQueue.main.async {
                self.startAuthorized()
            }
        }
    }

    func stop() {
        guard running || engine.isRunning else { return }
        running = false
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        analysisQueue.async { [weak self] in
            self?.rollingSamples.removeAll(keepingCapacity: true)
            self?.processor?.reset()
        }
    }

    private func requestPermission(_ completion: @escaping (Bool) -> Void) {
        switch AVAudioSession.sharedInstance().recordPermission {
        case .granted:
            completion(true)
        case .denied:
            completion(false)
        case .undetermined:
            AVAudioSession.sharedInstance().requestRecordPermission(completion)
        @unknown default:
            completion(false)
        }
    }

    private func startAuthorized() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: [])
            try session.setPreferredSampleRate(48_000)
            try session.setActive(true)

            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                throw NSError(
                    domain: "BlakeTunerAudio",
                    code: 1,
                    userInfo: [NSLocalizedDescriptionKey: "Microphone format is unavailable."]
                )
            }

            let sampleRate = Int(format.sampleRate.rounded())
            analysisQueue.sync {
                processor = TunerProcessor(sampleRate: Int32(sampleRate))
                rollingSamples.removeAll(keepingCapacity: true)
            }

            input.installTap(
                onBus: 0,
                bufferSize: AVAudioFrameCount(Self.hopSize),
                format: format
            ) { [weak self] buffer, _ in
                self?.consume(buffer: buffer)
            }
            engine.prepare()
            try engine.start()
            running = true
        } catch {
            stop()
            onError?("Could not start microphone capture: \(error.localizedDescription)")
        }
    }

    private func consume(buffer: AVAudioPCMBuffer) {
        guard running || engine.isRunning else { return }
        guard let floatChannels = buffer.floatChannelData else { return }
        let frameCount = Int(buffer.frameLength)
        guard frameCount > 0 else { return }

        let channel = floatChannels[0]
        var samples = [Int16]()
        samples.reserveCapacity(frameCount)
        var energy: Double = 0
        for index in 0..<frameCount {
            let clamped = max(-1.0, min(1.0, channel[index]))
            energy += Double(clamped * clamped)
            samples.append(Int16(clamped * Float(Int16.max)))
        }

        let rms = Float(sqrt(energy / Double(frameCount)))
        DispatchQueue.main.async { [weak self] in self?.onSignal?(rms) }

        analysisQueue.async { [weak self] in
            self?.analyze(samples: samples)
        }
    }

    private func analyze(samples: [Int16]) {
        guard let processor else { return }
        rollingSamples.append(contentsOf: samples)
        if rollingSamples.count < Self.analysisSize { return }
        if rollingSamples.count > Self.analysisSize {
            rollingSamples = Array(rollingSamples.suffix(Self.analysisSize))
        }

        let kotlinSamples = KotlinShortArray(size: Int32(Self.analysisSize))
        for (index, value) in rollingSamples.enumerated() {
            kotlinSamples.set(index: Int32(index), value: value)
        }

        let active = settings
        guard let frame = processor.analyze(
            samples: kotlinSamples,
            mode: Int32(active.mode),
            preset: Int32(active.preset),
            selectedStringIndex: Int32(active.selectedStringIndex),
            a4Hz: active.a4Hz
        ) else { return }

        DispatchQueue.main.async { [weak self] in self?.onFrame?(frame) }
    }

    private static let analysisSize = 4096
    private static let hopSize = 2048
}

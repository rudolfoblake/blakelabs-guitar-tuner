import Foundation
import UIKit
import BlakeTunerShared

@MainActor
final class TunerViewModel: ObservableObject {
    @Published var listening = false
    @Published var mode = 0
    @Published var preset = 0
    @Published var selectedStringIndex = -1
    @Published var a4Hz = 440.0
    @Published var noteLabel = "—"
    @Published var frequencyHz: Double?
    @Published var cents = 0.0
    @Published var confidence: Float = 0
    @Published var signal: Float = 0
    @Published var status = 0
    @Published var errorMessage: String?

    let presetNames = ["Standard", "Drop D", "DADGAD"]
    let presetStrings = [
        ["E2", "A2", "D3", "G3", "B3", "E4"],
        ["D2", "A2", "D3", "G3", "B3", "E4"],
        ["D2", "A2", "D3", "G3", "A3", "D4"],
    ]

    private let audio = TunerAudioEngine()
    private var lastInTune = false

    init() {
        audio.onSignal = { [weak self] rms in
            guard let self else { return }
            self.signal = self.signalLevel(rms)
        }
        audio.onFrame = { [weak self] frame in
            guard let self else { return }
            self.noteLabel = frame.noteLabel
            self.frequencyHz = frame.frequencyHz
            self.cents = frame.cents
            self.confidence = frame.confidence
            self.status = Int(frame.status)
            let inTune = frame.status == 2
            if inTune && !self.lastInTune {
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }
            self.lastInTune = inTune
        }
        audio.onError = { [weak self] message in
            guard let self else { return }
            self.listening = false
            self.errorMessage = message
        }
        pushSettings()
    }

    func toggleListening() {
        errorMessage = nil
        if listening {
            audio.stop()
            listening = false
            clearMeasurement()
        } else {
            pushSettings()
            audio.start()
            listening = true
        }
    }

    func setMode(_ value: Int) {
        mode = value
        selectedStringIndex = -1
        clearMeasurement()
        pushSettings()
    }

    func setPreset(_ value: Int) {
        preset = value
        selectedStringIndex = -1
        clearMeasurement()
        pushSettings()
    }

    func selectString(_ index: Int) {
        selectedStringIndex = selectedStringIndex == index ? -1 : index
        clearMeasurement()
        pushSettings()
    }

    func adjustA4(_ delta: Double) {
        a4Hz = min(450, max(430, a4Hz + delta))
        clearMeasurement()
        pushSettings()
    }

    func stop() {
        audio.stop()
        listening = false
    }

    private func pushSettings() {
        audio.update(settings: .init(
            mode: mode,
            preset: preset,
            selectedStringIndex: selectedStringIndex,
            a4Hz: a4Hz
        ))
    }

    private func clearMeasurement() {
        noteLabel = "—"
        frequencyHz = nil
        cents = 0
        confidence = 0
        status = 0
        lastInTune = false
    }

    private func signalLevel(_ rms: Float) -> Float {
        min(1, max(0, (rms - 0.0005) / 0.025))
    }
}

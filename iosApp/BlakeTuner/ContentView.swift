import SwiftUI

struct ContentView: View {
    @StateObject private var model = TunerViewModel()
    private let lime = Color(red: 0.78, green: 1.0, blue: 0.20)

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    header
                    readout
                    gauge
                    meters
                    modeControl
                    if model.mode == 0 { guitarControls }
                    calibration
                    microphoneButton
                    privacyFooter
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 18)
            }
        }
        .foregroundStyle(.white)
        .alert("Microphone", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { model.errorMessage = nil }
        } message: {
            Text(model.errorMessage ?? "")
        }
        .onDisappear { model.stop() }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text("BLAKE LABS")
                    .font(.caption.weight(.bold))
                    .tracking(2.2)
                    .foregroundStyle(lime)
                Text("GUITAR TUNER")
                    .font(.caption2.weight(.semibold))
                    .tracking(1.4)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text("OFFLINE")
                .font(.caption2.monospaced().weight(.bold))
                .foregroundStyle(lime)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(lime.opacity(0.1), in: Capsule())
        }
    }

    private var readout: some View {
        VStack(spacing: 5) {
            Text(model.noteLabel)
                .font(.system(size: 86, weight: .black, design: .rounded))
                .foregroundStyle(model.status == 2 ? lime : .white)
                .minimumScaleFactor(0.6)
            Text(model.frequencyHz.map { String(format: "%.2f Hz", $0) } ?? "Play a note")
                .font(.system(.body, design: .monospaced).weight(.medium))
                .foregroundStyle(.secondary)
            Text(statusText)
                .font(.headline.weight(.bold))
                .foregroundStyle(model.status == 2 ? lime : .white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    private var gauge: some View {
        VStack(spacing: 10) {
            HStack {
                Text("FLAT")
                Spacer()
                Text(String(format: "%+.1f cents", model.cents))
                    .foregroundStyle(model.status == 2 ? lime : .white)
                Spacer()
                Text("SHARP")
            }
            .font(.caption.monospaced().weight(.bold))
            .foregroundStyle(.secondary)

            GeometryReader { proxy in
                let width = proxy.size.width
                let center = width / 2
                let normalized = min(1, max(-1, model.cents / 50))
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.12)).frame(height: 8)
                    Rectangle().fill(lime.opacity(0.25))
                        .frame(width: max(8, width * 0.06), height: 18)
                        .position(x: center, y: 11)
                    Rectangle().fill(model.status == 2 ? lime : Color.white)
                        .frame(width: 3, height: 34)
                        .position(x: center + CGFloat(normalized) * (center - 8), y: 17)
                }
            }
            .frame(height: 34)
        }
        .padding(18)
        .background(Color.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var meters: some View {
        HStack(spacing: 12) {
            meter(title: "SIGNAL", value: Double(model.signal))
            meter(title: "LOCK", value: Double(model.confidence))
        }
    }

    private func meter(title: String, value: Double) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                Spacer()
                Text("\(Int(value * 100))%")
            }
            .font(.caption2.monospaced().weight(.bold))
            .foregroundStyle(.secondary)
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.09))
                    Capsule().fill(lime).frame(width: proxy.size.width * max(0, min(1, value)))
                }
            }
            .frame(height: 5)
        }
        .padding(14)
        .background(Color.white.opacity(0.045), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var modeControl: some View {
        Picker("Mode", selection: Binding(
            get: { model.mode },
            set: { model.setMode($0) }
        )) {
            Text("Guitar").tag(0)
            Text("Chromatic").tag(1)
        }
        .pickerStyle(.segmented)
    }

    private var guitarControls: some View {
        VStack(spacing: 14) {
            Picker("Tuning", selection: Binding(
                get: { model.preset },
                set: { model.setPreset($0) }
            )) {
                ForEach(model.presetNames.indices, id: \.self) { index in
                    Text(model.presetNames[index]).tag(index)
                }
            }
            .pickerStyle(.menu)
            .tint(lime)
            .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 7) {
                ForEach(model.presetStrings[model.preset].indices, id: \.self) { index in
                    let active = model.selectedStringIndex == index
                    Button(model.presetStrings[model.preset][index]) {
                        model.selectString(index)
                    }
                    .font(.caption.monospaced().weight(.bold))
                    .foregroundStyle(active ? .black : .white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(active ? lime : Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.04), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var calibration: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text("REFERENCE")
                    .font(.caption2.monospaced().weight(.bold))
                    .foregroundStyle(.secondary)
                Text("A4  \(Int(model.a4Hz)) Hz")
                    .font(.headline.monospaced().weight(.bold))
            }
            Spacer()
            Button("−") { model.adjustA4(-1) }
                .buttonStyle(CalibrationButtonStyle())
            Button("+") { model.adjustA4(1) }
                .buttonStyle(CalibrationButtonStyle())
        }
    }

    private var microphoneButton: some View {
        Button {
            model.toggleListening()
        } label: {
            HStack {
                Image(systemName: model.listening ? "stop.fill" : "mic.fill")
                Text(model.listening ? "STOP LISTENING" : "START TUNING")
                    .font(.headline.weight(.black))
            }
            .foregroundStyle(.black)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 17)
            .background(lime, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
    }

    private var privacyFooter: some View {
        Text("Audio is analyzed in memory on this iPhone. No ads. No trackers. No account. No network required.")
            .font(.caption)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .padding(.bottom, 10)
    }

    private var statusText: String {
        switch model.status {
        case 1: return "Tune up"
        case 2: return "In tune"
        case 3: return "Tune down"
        default: return model.listening ? "Listening" : "Ready"
        }
    }
}

private struct CalibrationButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title3.weight(.bold))
            .frame(width: 42, height: 38)
            .background(Color.white.opacity(configuration.isPressed ? 0.16 : 0.08), in: RoundedRectangle(cornerRadius: 12))
    }
}

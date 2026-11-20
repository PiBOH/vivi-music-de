package com.music.vivi.desktop.utils

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import java.io.File

/**
 * Skiko picks Direct3D 12 on Windows and falls back to OpenGL when the GPU has no
 * D3D12 device. On some old drivers that OpenGL path blocks the AWT thread in
 * `glFinish` for seconds per frame, which is what made resizing unusable. The
 * software renderer is slower on paper but keeps the UI responsive there.
 *
 * The choice is a file read before Skiko is initialized, so it can be applied at
 * the start of main without touching the settings repository (whose I/O starts
 * later and must not delay the renderer decision).
 */
object RenderMode {

    private val file: File by lazy { File(DesktopPathProvider.configDir, "renderer.txt") }

    fun isSoftware(): Boolean = runCatching {
        file.isFile && file.readText().trim().equals(SOFTWARE, ignoreCase = true)
    }.getOrDefault(false)

    fun setSoftware(enabled: Boolean) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(if (enabled) SOFTWARE else AUTO)
        }
    }

    /**
     * Applies the saved choice unless `--render-api=` overrides it afterwards. With
     * no saved choice, a Windows machine whose GPU cannot create a Direct3D 12
     * device gets the software renderer: Skiko's next choice is OpenGL, and on the
     * old drivers that path blocks in `glFinish` for seconds per frame.
     */
    fun applyStartupOverride() {
        val windows = System.getProperty("os.name").orEmpty().startsWith("Windows", true)
        when {
            isSoftware() -> System.setProperty("skiko.renderApi", "SOFTWARE")
            windows && !Direct3D12.isAvailable() -> {
                System.err.println("[render] no Direct3D 12 device; using the software renderer")
                System.setProperty("skiko.renderApi", "SOFTWARE")
            }
        }
    }

    private const val SOFTWARE = "software"
    private const val AUTO = "auto"
}

/**
 * One COM call into d3d12.dll: whether the machine can create a device. It runs
 * before Skiko, is wrapped in `runCatching`, and answers `true` when the probe
 * itself cannot run, so an unknown host keeps the normal Direct3D preference.
 */
private object Direct3D12 {

    private const val D3D_FEATURE_LEVEL_11_0 = 0xB000

    fun isAvailable(): Boolean = try {
        val library = NativeLibrary.getInstance("d3d12")
        val createDevice = library.getFunction("D3D12CreateDevice")

        // IID_ID3D12Device = {189819f1-1db6-4b57-be54-1821339b85f7}, in GUID layout.
        val iid = Memory(16)
        iid.setInt(0, 0x189819f1)
        iid.setShort(4, 0x1db6.toShort())
        iid.setShort(6, 0x4b57.toShort())
        iid.write(8, byteArrayOf(0xbe.toByte(), 0x54, 0x18, 0x21, 0x33, 0x9b.toByte(), 0x85.toByte(), 0xf7.toByte()), 0, 8)

        val devicePointer = Memory(Native.POINTER_SIZE.toLong()).apply { clear() }
        val result = createDevice.invokeInt(
            arrayOf(null, D3D_FEATURE_LEVEL_11_0, iid, devicePointer)
        )
        // The device reference is intentionally not released: this runs once before
        // the window exists, and the process keeps one device for its whole life.
        result >= 0 && devicePointer.getPointer(0) != null
    } catch (_: Throwable) {
        true
    }
}

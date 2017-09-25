package cn.kona.logging.log4j2

import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter

@Plugin(name = "ProcessIdPatternConverter", category = "Converter")
@ConverterKeys("pid", "processId")
@Suppress("unused")
class ProcessIdPatternConverter private constructor(options: Array<String>) : LogEventPatternConverter("Process ID", "pid") {

    private val pid: String = try {
        // likely works on most platforms
        ManagementFactory.getRuntimeMXBean().name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    } catch (ex: Exception) {
        try {
            // try a Linux-specific way
            File("/proc/self").canonicalFile.name
        } catch (ignoredUseDefault: IOException) {
            if (options.isNotEmpty()) options[0] else "???"
        }

    }

    override fun format(event: LogEvent, toAppendTo: StringBuilder) {
        toAppendTo.append(pid)
    }

    companion object {
        @JvmStatic
        fun newInstance(options: Array<String>): ProcessIdPatternConverter {
            return ProcessIdPatternConverter(options)
        }
    }
}

import java.io.File
import java.util.Comparator


class FileSorter(var type:String? = "N") : Comparator<File> {
    override fun compare(arg0: File, arg1: File): Int {
        return when {
            this.type == "N" -> {
                when {
                    arg0.nameWithoutExtension.toLowerCase() > arg1.nameWithoutExtension.toLowerCase() -> 1
                    arg0.nameWithoutExtension.toLowerCase() < arg1.nameWithoutExtension.toLowerCase() -> -1
                    else -> 0
                }
            }
            this.type == "M" -> {
                when {
                    arg0.lastModified() > arg1.lastModified() -> 1
                    arg0.lastModified() < arg1.lastModified() -> -1
                    else -> 0
                }
            }
            this.type == "T" -> {
                when {
                    arg0.length() > arg1.length() -> 1
                    arg0.length() < arg1.length() -> -1
                    else -> 0
                }
            }
            else -> {
                0
            }
        }
    }
}
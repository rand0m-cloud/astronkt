import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.astronkt.dclassmacro.main
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class CompileTest {
    @Test
    fun `generated code compiles successfully`() {
        File("build/generated/dclassbindings").deleteRecursively()

        main(arrayOf("test.dc", "build/generated/dclassbindings"))

        val files = File("build/generated/dclassbindings").listFiles()!!.map {
            SourceFile.fromPath(it)
        }

        val result = KotlinCompilation().apply {
            sources = files
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.classLoader.loadClass("GameSpec.DistributedTestObject")
        result.classLoader.loadClass("GameSpec.ClassSpecKt")
    }
}
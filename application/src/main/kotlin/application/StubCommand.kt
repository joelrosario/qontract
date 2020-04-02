package application

import run.qontract.core.utilities.readFile
import run.qontract.fake.ContractFake
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "stub", version = ["0.1.0"],
        mixinStandardHelpOptions = true,
        description = ["Start a stub server with contract"])
class StubCommand : Callable<Void> {
    lateinit var contractFake: ContractFake

    @Option(names = ["--path"], description = ["Contract location"], required = true)
    lateinit var path: String

    @Option(names = ["--host"], description = ["Host"], defaultValue = "localhost")
    lateinit var host: String

    @Option(names = ["--port"], description = ["Port"], defaultValue = "9000")
    lateinit var port: Integer

    override fun call(): Void? {
        val contractGherkin = readFile(path)
        addShutdownHook()
        contractFake = ContractFake(contractGherkin, host, port.toInt())
        println("Stub server is running on http://$host:$port. Ctrl + C to stop.")
        while (true) {
            Thread.sleep(1000)
        }

        return null
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                try {
                    println("Shutting down stub server")
                    contractFake?.close()
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        })
    }
}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.{ActorMaterializer, TLSClientAuth}
import org.ddahl.rscala.RClient
import java.io.InputStream
import java.security.{ SecureRandom, KeyStore }
import javax.net.ssl.{ SSLContext, TrustManagerFactory, KeyManagerFactory }

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ Route, Directives }
import akka.http.scaladsl.{ ConnectionContext, HttpsConnectionContext, Http }
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import scala.concurrent.Await
import scala.util.{Failure, Success}
import java.io.PrintWriter

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.server.directives._
import ContentTypeResolver.Default
import Main.system
import akka.http.scaladsl.server.Directives._
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.{SSLContext, SSLParameters}

import scala.collection.parallel.immutable

object Main extends App {

  val host = "37.0.0.9"
  val port = 9000

  implicit val system: ActorSystem = ActorSystem(name = "todoapi")
  implicit val mat = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val password: Array[Char] = "change me".toCharArray // do not store passwords in code, read them from somewhere safe!

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")

  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)

  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

  val processroute : Route  = path("hello") {
    get {
      parameter("inputfile") { inputfile =>
        complete(inputfile)
        val R = RClient()
        R.eval("source('C:/Users/wsadmin/Documents/samplezip.R')")
        getFromFile("C:/Users/wsadmin/Desktop/sample.zip")
      }
    }
  }

  // sets default context to HTTPS – all Http() bound servers for this ActorSystem will use HTTPS from now on
  Http().setDefaultServerHttpContext(https)
   val binding = Http().bindAndHandle(processroute, host, port,https)
  binding.onComplete {
    case Success(_) => println("Success!")
    case Failure(error) => println(s"Failed: ${error.getMessage}")
  }

  import scala.concurrent.duration._
  Await.result(binding, 3.seconds)
}
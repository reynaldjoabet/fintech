package example
//import openbanking.client.model
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.{
  CodecMakerConfig,
  JsonCodecMaker
}

import finicity.models.*
import finicity.api.IdentityApi
//import finicity.api.AnalyticsApi
import finicity.api.AccountsApi
import finicity.api.AppRegistrationApi
import finicity.api.AuthenticationApi
import finicity.api.VerifyAssetsApi
import finicity.api.BankStatementsApi
import finicity.api.CashFlowApi
import finicity.api.ConnectApi
import finicity.api.ConsumersApi
import finicity.api.CustomersApi
import finicity.api.PortfoliosApi
import finicity.api.ReportsApi
import finicity.api.InstitutionsApi
import finicity.api.PayStatementsApi
import finicity.api.PaymentsApi
import finicity.api.TransactionsApi
import finicity.api.TxPushApi
import finicity.api.VerifyIncomeAndEmploymentApi

import org.bouncycastle.crypto.generators.BCrypt
import org.bouncycastle.crypto.generators.SCrypt
import org.bouncycastle.crypto.generators.DESKeyGenerator
import org.bouncycastle.crypto.generators.DESedeKeyGenerator
import org.bouncycastle.crypto.generators.DHKeyPairGenerator
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.KDF1BytesGenerator
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.generators.MGF1BytesGenerator
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.generators.SM2KeyPairGenerator
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator
import org.bouncycastle.crypto.generators.X448KeyPairGenerator
import org.bouncycastle.crypto.generators.BaseKDFBytesGenerator
import org.bouncycastle.crypto.generators.DHParametersGenerator
import org.bouncycastle.crypto.generators.ECCSIKeyPairGenerator
import org.bouncycastle.crypto.generators.Ed448KeyPairGenerator
import org.bouncycastle.crypto.generators.MLDSAKeyPairGenerator
import org.bouncycastle.crypto.generators.MLKEMKeyPairGenerator
import org.bouncycastle.crypto.generators.DSAParametersGenerator
import org.bouncycastle.crypto.generators.SLHDSAKeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.generators.DHBasicKeyPairGenerator
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator
import org.bouncycastle.crypto.generators.DSTU4145KeyPairGenerator
import org.bouncycastle.crypto.generators.GOST3410KeyPairGenerator
import org.bouncycastle.crypto.generators.KDFCounterBytesGenerator
import org.bouncycastle.crypto.generators.EphemeralKeyPairGenerator
import org.bouncycastle.crypto.generators.KDFFeedbackBytesGenerator
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator
import org.bouncycastle.crypto.generators.PKCS5S1ParametersGenerator
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.generators.CramerShoupKeyPairGenerator
object Main extends App with Greeting {
  println(greeting)

  val account = CustomerAccount(
    id = "123",
    number = "456",
    accountNumberDisplay = "456",
    name = "My Account",
    balance = Some(1000.0),
    `type` = "Checking",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1625097600L,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = Some(1)
  )

}

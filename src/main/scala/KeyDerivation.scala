package example

import java.nio.ByteBuffer
import java.nio.CharBuffer

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_d
import org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_i
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.Digest

/**
  * The Argon2 key derivation function (KDF) is a modern algorithm to shade and hash passwords.
  *
  * <p>The default implementation ({@code argon2id}) is designed to use both memory and cpu to make
  * brute force attacks unfeasible.</p>
  *
  * <p>The defaults are taken from <a
  * href="https://argon2-cffi.readthedocs.io/en/stable/parameters.html">argon2-cffi.readthedocs.io</a>.
  * The RFC suggests to use 1 GiB of memory for frontend and 4 GiB for backend authentication.</p>
  *
  * <p>Default values are taken from <a
  * href="https://datatracker.ietf.org/doc/draft-irtf-cfrg-argon2/?include_text=1">draft-irtf-cfrg-argon2-13</a>.
  * This implementation is using the parameters from section 4, paragraph 2 (memory constrained
  * environment).</p>
  */
object KeyDerivation {}

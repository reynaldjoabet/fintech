package example

enum TestType {
  case GRE, Duolingo, TOEFL, IELTS, PTE, SAT, ACT
}

final case class TestScore(testType: TestType, score: Int)

final case class IPTable(
    permission: String,
    ipAddress: String,
    protocol: String,
    destination: String,
    portRange: String
)

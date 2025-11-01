import example.Hello
import munit.FunSuite

class HelloSpec extends munit.FunSuite {
  test("say hello") {
    assertEquals(Hello.greeting, "hello")
  }

  test("say hello is not hi") {
    assertNotEquals(Hello.greeting, "hi")
  }

  test("say hello is not empty") {
    assert(Hello.greeting.nonEmpty)
  }

  test("say hello contains h") {
    assert(Hello.greeting.contains("h"))
  }

  test("say hello length is 5") {
    assertEquals(Hello.greeting.length, 5)
  }

  test("say hello is lowercase") {
    assert(Hello.greeting.forall(_.isLower))
  }

}

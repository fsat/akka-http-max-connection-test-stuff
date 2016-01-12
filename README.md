# Reproducer for Max Connection Setting - Akka HTTP 2.0.1

HTTP().bindAndHandle() does not enforce the max connection setting - Akka HTTP 2.0.1

**Setup**

* Open a terminal and execute `sbt run` - this will start an Akka HTTP server with max connection limited to `1` 
  configured through custom `ServerSettings`
* Open a new terminal, subscribe to a SSE: `curl localhost:7070/events` - this will open the first connection
* Open *another* new terminal, subscribe to a SSE: `curl localhost:7070/events` - this will open the 2nd connection 
  which *should fail*, but it proceeds successfully anyway.

I've also attempted to create a test reproducer `test.maxconn.MaxConnectionTest` - run `sbt test` to invoke this.

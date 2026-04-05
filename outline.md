 # Prometheus and Grafana Plan

* Prometheus
* Prometheus Exposition Format
* Java Monitoring Options in Spring and Quarkus (Micrometer, right?)
* Prometheus Operator in Kubernetes
* Prometheus Tie-in to Grafana
* PromQL
* Prometheus Architecture
* Fitting Telemetry with Prometheus
* Using Prometheus with Argo Rollouts for Canary
* Using Prometheus with OpenTelemetry
* Common Metrics that you need to cover today, i.e. where to start on Monday


1. What Is Monitoring (and What It Is Not) (10 min)
	•	Monitoring vs logging vs tracing
	•	Why metrics exist at all
	•	The cost of not monitoring
	•	“Pretty dashboards” vs actionable insight
	•	Observability as a feedback loop, not a tool


2. Metrics as a First-Class System Concept (10 min)
	•	Counters, gauges, histograms, summaries
	•	Cardinality and why it will hurt you
	•	Pull vs push models
	•	Why Prometheus chose pull
	•	The difference between events and state


3. Prometheus Architecture (15 min)
	•	Prometheus server
	•	Targets and exporters
	•	Scrape intervals and time series
	•	Labels as dimensions
	•	Local storage vs long-term storage
	•	Federation (and when not to use it)

Key mental model:

Prometheus is a time-series database with opinions

4. JVM Monitoring and JMX (15 min)

This is where your talk really stands out.
	•	What JMX actually is
	•	MBeans, attributes, and operations
	•	Common JVM metrics:
	•	Heap / non-heap memory
	•	GC behavior
	•	Threads
	•	Class loading
	•	JMX Exporter:
	•	How it works
	•	Mapping MBeans to Prometheus metrics
	•	Anti-patterns:
	•	Exposing everything
	•	Over-labeling
	•	Treating JMX as a logging system


5. Instrumenting Applications (10 min)
	•	JVM apps (Micrometer)
	•	Spring Boot Actuator
	•	Quarkus metrics
	•	Custom business metrics:
	•	Why “requests/sec” isn’t enough
	•	Domain-level metrics vs technical metrics
	•	The difference between instrumentation and measurement design


6. PromQL: Asking the Right Questions
	•	Time ranges and rates
	•	Aggregation (sum, avg, max, by)
	•	Recording rules
	•	Alerting rules (briefly)
	•	Common mistakes:
	•	Instant vectors vs range vectors
	•	Averaging latency incorrectly
	•	Ignoring reset behavior


7. Grafana: Dashboards That Tell the Truth (10 min)
	•	Grafana as a visualization layer
	•	Dashboard design principles:
	•	Start with questions, not charts
	•	Golden Signals
	•	RED / USE methodologies
	•	Variables and templating
	•	Avoiding dashboard sprawl
	•	Why most dashboards fail in incidents

8. Putting It Together: From JVM to Production Insight (10 min)
	•	JVM → JMX → Prometheus → Grafana
	•	Correlating metrics across layers
	•	Detecting:
	•	Memory pressure
	•	Thread starvation
	•	GC thrashing
	•	Load vs capacity
	•	When metrics cannot explain behavior

9. What Prometheus Is Not (5 min)
	•	Not a logging system
	•	Not a tracing system
	•	Not a monitoring silver bullet
	•	How it fits with:
	•	OpenTelemetry
	•	Distributed tracing
	•	Alerting systems

Optional Demos (you can choose 1–2)
	•	JVM app with JMX exporter
	•	Prometheus scraping live metrics
	•	Grafana dashboard built from scratch
	•	Inducing GC pressure and watching metrics change


# Demo Instructions

## Demo: Prometheus and PromQL

1. In a terminal, go into the _demos/prometheus_ directory
2. Run `docker-compose up -d`
3. Open in a browser http://localhost:9090 for prometheus
4. Open in a browser http://localhost:3000 for grafana
5. In Grafana, login with _admin_ for the user, and _admin_ for the password. When it asks you to generate a new password, select _Skip_
6. In Prometheus Query the following PromQL
   * `prometheus_http_requests_total`
   * `rate(prometheus_http_requests_total[5m])`
   * `prometheus_tsdb_head_series`
7. In Grafana, add a connection to Prometheus so we can see a better view.

## Demo: Java Management Extensions (JMX)

1. In a terminal, go into the _demos/jmx-app_ directory

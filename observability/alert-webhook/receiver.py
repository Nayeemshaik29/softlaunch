# Tiny stand-in for Telegram/Slack/email: prints every alert Grafana sends.
# View with:  docker compose logs -f alert-webhook
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        for alert in body.get("alerts", []):
            icon = "🔥 FIRING  " if alert.get("status") == "firing" else "✅ RESOLVED"
            print(f"{icon} [{alert['labels'].get('severity', '-')}] {alert['annotations'].get('summary', alert['labels'].get('alertname'))}", flush=True)
        self.send_response(200)
        self.end_headers()

    def log_message(self, *args):  # hide the default access log line
        pass


HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()

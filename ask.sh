#!/bin/bash
# ./ask.sh "your question"          (override host with PORT=8081 ./ask.sh "...")
PORT="${PORT:-8080}" python3 - "$1" << 'PY'
import json,os,sys,urllib.request
port=os.environ.get("PORT","8080")
req=urllib.request.Request(f"http://localhost:{port}/v1/query",
    data=json.dumps({"question":sys.argv[1]}).encode(),
    headers={"Content-Type": "application/json"})
d=json.load(urllib.request.urlopen(req))
t=d["timing"]
print(f'\n  {"OK" if d["ok"] else "FAILED"}   total={t["totalMs"]}ms  (llm={t["llmMs"]}ms exec={t["execMs"]}ms)  SLA={t["sla"]}')
if d.get("error"): print(f'  error: {d["error"]}')
print(f'\n  SQL:\n    {d["sql"]}\n')
rows=d["rows"]
print(f'  {len(rows)} row(s)')
for r in rows[:8]: print("   ", json.dumps(r))
if len(rows)>8: print(f'    ... {len(rows)-8} more')
print()
PY

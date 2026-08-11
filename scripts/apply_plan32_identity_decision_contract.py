#!/usr/bin/env python3
from pathlib import Path

ROOT = Path.cwd()
SRC = ROOT / "app/src/main/java"
OUT = ROOT / "build/plan32/batch002_identity_decision_contract.md"
files = list(SRC.rglob("*.kt"))

def read(p):
    return p.read_text(encoding="utf-8")

model_file = next((p for p in files if "data class IdentityCandidateDecisionRequest" in read(p)), None)
if model_file is None:
    raise SystemExit("ERROR: IdentityCandidateDecisionRequest not found")

text = read(model_file)
start = text.find("data class IdentityCandidateDecisionRequest")
openp = text.find("(", start)
depth = 0
closep = None
for i in range(openp, len(text)):
    if text[i] == "(":
        depth += 1
    elif text[i] == ")":
        depth -= 1
        if depth == 0:
            closep = i
            break
if closep is None:
    raise SystemExit("ERROR: cannot bound IdentityCandidateDecisionRequest")

block = text[start:closep+1]
insertions = []
if "expectedSourcePdid" not in block:
    insertions.append('    @SerialName("expected_source_pdid")\n    val expectedSourcePdid: String = "",\n\n')
if "expectedTargetPdid" not in block:
    insertions.append('    @SerialName("expected_target_pdid")\n    val expectedTargetPdid: String = "",\n\n')
if "expectedUpdatedAt" not in block:
    insertions.append('    @SerialName("expected_updated_at")\n    val expectedUpdatedAt: String? = null,\n\n')

if insertions:
    marker = '@SerialName("decision_note")'
    where = block.find(marker)
    if where < 0:
        raise SystemExit("ERROR: decision_note field anchor not found")
    line = block.rfind("\n", 0, where) + 1
    block = block[:line] + "".join(insertions) + block[line:]
    text = text[:start] + block + text[closep+1:]
    model_file.write_text(text, encoding="utf-8")

patched = 0
for p in files:
    t = read(p)
    if "IdentityCandidateDecisionRequest(" not in t or "candidate: IdentityCandidateDetail" not in t:
        continue

    cursor = 0
    output = []
    changed = False
    while True:
        idx = t.find("IdentityCandidateDecisionRequest(", cursor)
        if idx < 0:
            output.append(t[cursor:])
            break

        output.append(t[cursor:idx])
        op = t.find("(", idx)
        depth = 0
        cp = None
        for j in range(op, len(t)):
            if t[j] == "(":
                depth += 1
            elif t[j] == ")":
                depth -= 1
                if depth == 0:
                    cp = j
                    break
        if cp is None:
            raise SystemExit("ERROR: malformed IdentityCandidateDecisionRequest call")

        call = t[idx:cp+1]
        nearby = t[max(0, idx-2500):idx]
        is_decision = (
            "candidate: IdentityCandidateDetail" in nearby and
            (
                "confirmIdentityCandidate" in nearby or
                "rejectIdentityCandidate" in nearby or
                "reopenIdentityCandidate" in nearby
            )
        )

        if is_decision and "expectedSourcePdid" not in call:
            inner = call[len("IdentityCandidateDecisionRequest("):-1].lstrip()
            call = (
                "IdentityCandidateDecisionRequest(\n"
                "                    expectedSourcePdid = candidate.sourcePdid,\n"
                "                    expectedTargetPdid = candidate.targetPdid,\n"
                "                    expectedUpdatedAt = candidate.updatedAt,\n"
                "                    " + inner +
                ")"
            )
            patched += 1
            changed = True

        output.append(call)
        cursor = cp + 1

    if changed:
        p.write_text("".join(output), encoding="utf-8")

alltext = "\n".join(read(p) for p in files)
required = [
    'SerialName("expected_source_pdid")',
    'SerialName("expected_target_pdid")',
    'SerialName("expected_updated_at")',
    "expectedSourcePdid = candidate.sourcePdid",
    "expectedTargetPdid = candidate.targetPdid",
    "expectedUpdatedAt = candidate.updatedAt",
]
missing = [x for x in required if x not in alltext]
OUT.parent.mkdir(parents=True, exist_ok=True)
if missing:
    OUT.write_text("# Batch 002 failed\n\n" + "\n".join("- " + x for x in missing), encoding="utf-8")
    raise SystemExit("ERROR: identity decision contract still incomplete")

OUT.write_text(
    "# Batch 002 passed\n\n"
    "- expected_source_pdid is sent.\n"
    "- expected_target_pdid is sent.\n"
    "- expected_updated_at is sent.\n"
    "- decision_note remains preserved.\n"
    "- Newly patched constructor calls: %d\n" % patched,
    encoding="utf-8",
)
print(OUT.read_text())

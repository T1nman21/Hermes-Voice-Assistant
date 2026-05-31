"""
Rocky Python Execution Engine
Runs user code in a sandboxed environment with stdout/stderr capture.
"""
import sys
import io
import traceback
import json
import math
import re
import datetime
import collections
import itertools
import csv
import hashlib
import base64
import random
import string
import urllib.parse
import html


def execute(code: str) -> str:
    """Execute Python code and return JSON result with stdout, stderr, success."""
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    captured_stdout = io.StringIO()
    captured_stderr = io.StringIO()

    result = {
        "success": False,
        "output": "",
        "error": None
    }

    try:
        sys.stdout = captured_stdout
        sys.stderr = captured_stderr

        # Create a sandbox globals dict with useful builtins
        sandbox = {
            "__builtins__": __builtins__,
            "math": math,
            "re": re,
            "datetime": datetime,
            "json": json,
            "collections": collections,
            "itertools": itertools,
            "csv": csv,
            "hashlib": hashlib,
            "base64": base64,
            "random": random,
            "string": string,
            "urllib": urllib,
            "html": html,
        }

        exec(code, sandbox)

        result["success"] = True
        result["output"] = captured_stdout.getvalue()[:8000]

        stderr_val = captured_stderr.getvalue()
        if stderr_val:
            result["error"] = stderr_val[:2000]

    except Exception:
        result["success"] = False
        result["output"] = captured_stdout.getvalue()[:4000]
        result["error"] = traceback.format_exc()[:2000]

    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr

    return json.dumps(result)


def version() -> str:
    return f"Python {sys.version}"

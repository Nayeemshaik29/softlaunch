package com.softlaunch.ai.openers;

import java.util.List;

/** source = "ai" (written by the LLM) or "fallback" (templates, when the AI is slow, down or gave bad output). */
public record OpenersResponse(List<String> openers, String source) {
}

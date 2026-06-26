{{#hasChunks}}
以下是与用户问题相关的代码笔记片段，请基于这些代码进行审查：

{{#chunks}}
---
[来源 {{index}}：《{{title}}}》]
{{text}}

{{/chunks}}
{{#truncated}}
（注：检索结果较多，以上仅展示最相关的 {{chunks.size}} 个片段，可能不完整）
{{/truncated}}
{{/hasChunks}}
{{^hasChunks}}
（未检索到相关代码笔记片段，如你需要代码审查，请先确保相关笔记已保存并向量化）
{{/hasChunks}}

用户问题：{{question}}

{{#hasChunks}}
以下是与用户问题相关的笔记片段（按语义相关度排序），请仔细阅读后回答：

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
（未检索到相关笔记片段，请告知用户无法从笔记中找到相关信息）
{{/hasChunks}}

用户问题：{{question}}

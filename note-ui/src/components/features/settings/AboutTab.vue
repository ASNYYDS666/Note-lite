<template>
  <div class="tab-content">
    <h3 class="about-title">Note-lite</h3>
    <p class="about-version">Version 1.0.0</p>
    <p class="about-desc">个人笔记管理 IDE，含 AI RAG 知识库问答</p>
  </div>
</template>

<style scoped>
.tab-content {
  padding: 20px;
  text-align: center;
}

.about-title {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--primary);
  margin: 0 0 8px;
}

.about-version {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  margin: 0 0 12px;
}

.about-desc {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--outline);
  margin: 0;
}
</style>

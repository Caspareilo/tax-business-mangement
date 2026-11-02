<template>
  <div class="not-found">
    <div class="code">404</div>
    <h1>页面不存在</h1>
    <p>访问的地址不存在或已被移除。</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回分析页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--tax-bg);
  color: var(--tax-text-secondary);
}

.code {
  font-size: 64px;
  font-weight: 600;
  color: var(--tax-border-strong);
  letter-spacing: 0.08em;
}

.not-found h1 {
  font-size: 20px;
  color: var(--tax-text);
}

.not-found p {
  margin-bottom: 10px;
  font-size: 13px;
}
</style>

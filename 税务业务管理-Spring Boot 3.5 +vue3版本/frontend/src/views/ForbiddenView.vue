<template>
  <div class="page">
    <div class="card" style="padding: 64px; text-align: center">
      <div class="code">403</div>
      <h1 class="page-title" style="margin-top: 10px">无权访问该页面</h1>
      <p class="page-description">当前账号没有该功能权限。如确有需要，请联系管理员调整角色权限。</p>
      <el-button type="primary" style="margin-top: 22px" @click="$router.push('/dashboard')">返回分析页</el-button>
    </div>
  </div>
</template>

<style scoped>
.code {
  font-size: 56px;
  font-weight: 600;
  color: var(--tax-border-strong);
  letter-spacing: 0.06em;
}
</style>

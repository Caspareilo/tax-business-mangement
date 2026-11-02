<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import loginIllustration from '@/assets/login-illustration.svg'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const activeTab = ref<'account' | 'role'>('account')

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, max: 64, message: '账号长度为 3-64 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
}

const demoAccounts = [
  { username: 'admin', label: '管理员', description: '用户、角色、全业务与审计' },
  { username: 'clerk', label: '经办人', description: '申报新建、提交和撤回' },
  { username: 'reviewer', label: '审核人', description: '待办审核、通过和退回' },
  { username: 'analyst', label: '分析员', description: '统计查看与数据导出' },
  { username: 'viewer', label: '只读', description: '工作台与业务只读查看' },
]

async function handleSubmit(): Promise<boolean> {
  if (submitting.value) return false
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return false
  submitting.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
    return true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请检查账号和密码')
    return false
  } finally {
    submitting.value = false
  }
}

async function quickLogin(account: (typeof demoAccounts)[number]) {
  activeTab.value = 'account'
  form.username = account.username
  form.password = 'Demo@1234'
  await nextTick()
  await handleSubmit()
}
</script>

<template>
  <div class="login-screen">
    <main class="login-shell">
      <section class="login-visual" aria-labelledby="loginBrandTitle">
        <div class="login-visual-inner">
          <h1 id="loginBrandTitle">税务业务管理<br />高效申报与审核</h1>
          <img
            class="login-illustration"
            :src="loginIllustration"
            alt="税务申报、审核、统计和纳税人档案组成的业务管理插画"
          />
          <ul class="login-feature-list" aria-label="系统能力">
            <li>申报</li>
            <li>审核</li>
            <li>统计</li>
            <li>审计</li>
          </ul>
          <p class="login-note">课程设计演示系统 · 数据均为虚构</p>
          <p class="login-copyright">Designed by 吴学孟 · 仅限课程演示使用</p>
        </div>
      </section>

      <section class="login-main-area" aria-labelledby="loginHeading">
        <div class="login-card-modern">
          <h2 id="loginHeading">税务业务管理平台</h2>

          <div class="login-tabs-modern" role="tablist" aria-label="登录方式">
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'account'"
              :class="{ active: activeTab === 'account' }"
              @click="activeTab = 'account'"
            >
              账号登录
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'role'"
              :class="{ active: activeTab === 'role' }"
              @click="activeTab = 'role'"
            >
              角色说明
            </button>
          </div>

          <div v-show="activeTab === 'account'" class="login-tab-content">
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="form.username"
                  :prefix-icon="User"
                  size="large"
                  placeholder="请输入登录账号"
                  autocomplete="username"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="form.password"
                  :prefix-icon="Lock"
                  type="password"
                  size="large"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                  show-password
                  @keyup.enter="handleSubmit"
                />
              </el-form-item>
              <el-button
                class="login-submit-modern"
                type="primary"
                size="large"
                :loading="submitting"
                @click="handleSubmit"
              >
                登录
              </el-button>
            </el-form>

            <section class="test-account-panel" aria-labelledby="testAccountTitle">
              <div class="test-account-head">
                <strong id="testAccountTitle">测试账号 · 点击即登录</strong>
                <span>仅演示环境</span>
              </div>
              <div class="test-account-grid">
                <button
                  v-for="account in demoAccounts"
                  :key="account.username"
                  type="button"
                  class="test-account"
                  :disabled="submitting"
                  :title="`${account.username} / Demo@1234，点击自动登录`"
                  :aria-label="`以${account.label} ${account.username} 登录`"
                  @click="quickLogin(account)"
                >
                  <span>{{ account.label }}</span>
                  <small>{{ account.username }}</small>
                </button>
              </div>
            </section>
          </div>

          <div v-show="activeTab === 'role'" class="login-tab-content">
            <p class="role-guide-hint">回到“账号登录”后，点击测试账号即可进入对应角色。</p>
            <div class="role-guide-modern">
              <div v-for="account in demoAccounts" :key="account.username" class="role-guide-item">
                <span class="role-guide-icon">{{ account.label.slice(0, 1) }}</span>
                <span>
                  <strong>{{ account.label }} · {{ account.username }}</strong>
                  <small>{{ account.description }}</small>
                </span>
              </div>
            </div>
          </div>

          <footer class="login-footer-note">本入口仅用于课程教学与演示，请勿录入真实纳税人或个人信息</footer>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.login-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 9px 40px;
  overflow: auto;
  background: #ffffff;
}

.login-shell {
  width: min(1160px, calc(100vw - 80px));
  height: min(760px, calc(100vh - 18px));
  min-height: 640px;
  display: grid;
  grid-template-columns: 45.4% 54.6%;
  overflow: hidden;
  border: 2px solid #e7e7e7;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 10px 34px rgba(36, 64, 102, 0.05);
}

.login-visual {
  min-width: 0;
  overflow: hidden;
  padding: clamp(46px, 7.2vh, 68px) 0 36px 48px;
  background: #fafafa;
}

.login-visual-inner {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.login-visual h1 {
  margin: 0;
  color: #093157;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  font-size: 40px;
  font-weight: 400;
  line-height: 1.45;
  letter-spacing: 0;
}

.login-illustration {
  width: 590px;
  max-width: none;
  height: auto;
  margin: 10px 0 8px -30px;
  display: block;
}

.login-feature-list {
  display: flex;
  gap: 0;
  margin: auto 0 0;
  padding: 0;
  color: #4f5964;
  font-size: 16px;
  list-style: none;
}

.login-feature-list li + li::before {
  margin: 0 10px;
  color: #8b939c;
  content: '·';
}

.login-note,
.login-copyright {
  margin: 9px 0 0;
  padding: 0;
  border: 0;
  color: #9aa1a9;
  font-size: 11px;
  line-height: 1.5;
}

.login-copyright {
  margin-top: 4px;
  color: #a8adb3;
  font-size: 10px;
  letter-spacing: 0.02em;
}

.login-main-area {
  min-width: 0;
  padding-left: 80px;
  background: #ffffff;
}

.login-card-modern {
  width: 430px;
  max-width: 100%;
  height: 100%;
  padding: clamp(48px, 8vh, 74px) 0 30px;
  display: flex;
  flex-direction: column;
}

.login-card-modern h2 {
  margin: 0 0 56px;
  color: #093157;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  font-size: 30px;
  font-weight: 500;
  line-height: 1.3;
}

.login-tabs-modern {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 48px;
  border-bottom: 1px solid #e8e8e8;
}

.login-tabs-modern button {
  position: relative;
  padding: 0 0 14px;
  border: 0;
  background: transparent;
  color: #222222;
  font-family: inherit;
  font-size: 18px;
  font-weight: 400;
  text-align: left;
  cursor: pointer;
}

.login-tabs-modern button::after {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 88px;
  height: 2px;
  background: transparent;
  content: '';
}

.login-tabs-modern button.active {
  color: #706ce8;
}

.login-tabs-modern button.active::after {
  background: #706ce8;
}

.login-tab-content {
  flex: 1;
}

.login-card-modern :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-card-modern :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #555555;
  font-size: 14px;
  font-weight: 400;
}

.login-card-modern :deep(.el-form-item.is-required .el-form-item__label::before) {
  display: none;
}

.login-card-modern :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 13px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #d9dde1 inset;
}

.login-card-modern :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #aaa6ed inset;
}

.login-card-modern :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #706ce8 inset;
}

.login-card-modern :deep(.el-input__inner) {
  font-size: 14px;
}

.login-submit-modern {
  width: 100%;
  height: 48px;
  margin-top: 36px;
  border: 0;
  border-radius: 5px;
  background: #706ce8;
  color: #ffffff;
  font-size: 19px;
  letter-spacing: 0.08em;
}

.login-submit-modern:hover,
.login-submit-modern:focus {
  border-color: #5f5bd6;
  background: #5f5bd6;
  color: #ffffff;
}

.test-account-panel {
  margin-top: 18px;
  padding: 10px 11px 11px;
  border: 1px solid #e9e7f7;
  border-radius: 4px;
  background: #fbfaff;
}

.test-account-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  color: #626770;
  font-size: 11px;
}

.test-account-head strong {
  color: #706ce8;
  font-weight: 500;
}

.test-account-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 5px;
}

.test-account {
  min-width: 0;
  height: 34px;
  padding: 3px 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border: 1px solid #e2e0f5;
  border-radius: 3px;
  background: #ffffff;
  color: #333333;
  font-family: inherit;
  line-height: 1.2;
  cursor: pointer;
}

.test-account:hover,
.test-account:focus-visible {
  border-color: #706ce8;
  color: #706ce8;
}

.test-account:disabled {
  cursor: wait;
  opacity: 0.6;
}

.test-account span {
  font-size: 10px;
  white-space: nowrap;
}

.test-account small {
  max-width: 100%;
  overflow: hidden;
  color: #8b8f96;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-guide-hint {
  margin: 0 0 18px;
  color: var(--tax-muted);
  font-size: 13px;
}

.role-guide-modern {
  display: grid;
  gap: 8px;
}

.role-guide-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 9px 10px;
  border: 1px solid #ececec;
  border-radius: 3px;
}

.role-guide-icon {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f1f0ff;
  color: #706ce8;
  font-size: 11px;
}

.role-guide-item strong,
.role-guide-item small {
  display: block;
}

.role-guide-item strong {
  color: #323232;
  font-size: 12px;
  font-weight: 500;
}

.role-guide-item small {
  margin-top: 2px;
  color: #8d949b;
  font-size: 10px;
}

.login-footer-note {
  margin-top: auto;
  padding-top: 10px;
  color: #a0a5ab;
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}

@media (max-width: 1100px) and (min-width: 861px) {
  .login-screen {
    padding: 9px 20px;
  }

  .login-shell {
    width: calc(100vw - 40px);
    grid-template-columns: 47% 53%;
  }

  .login-visual {
    padding-left: 36px;
  }

  .login-visual h1 {
    font-size: 34px;
  }

  .login-illustration {
    width: 540px;
    margin-left: -26px;
  }

  .login-main-area {
    padding-left: 70px;
  }
}

@media (max-width: 860px) {
  .login-screen {
    min-height: 100vh;
    display: block;
    padding: 0;
    background: #ffffff;
  }

  .login-shell {
    width: 100%;
    height: auto;
    min-height: 100vh;
    display: block;
    overflow: visible;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .login-visual {
    padding: 28px 24px 24px;
    border-bottom: 1px solid #e8e8e8;
  }

  .login-visual-inner {
    display: block;
  }

  .login-visual h1 {
    font-size: 28px;
    line-height: 1.4;
  }

  .login-illustration,
  .login-feature-list,
  .login-note,
  .login-copyright {
    display: none;
  }

  .login-main-area {
    padding: 30px 24px 36px;
  }

  .login-card-modern {
    width: min(100%, 480px);
    height: auto;
    min-height: 0;
    margin: 0 auto;
    padding: 0;
  }

  .login-card-modern h2 {
    margin-bottom: 28px;
    font-size: 26px;
  }

  .login-tabs-modern {
    margin-bottom: 30px;
  }

  .login-submit-modern {
    margin-top: 22px;
  }

  .login-footer-note {
    margin-top: 24px;
  }
}

@media (max-width: 480px) {
  .login-visual {
    padding: 22px 18px 20px;
  }

  .login-visual h1 {
    font-size: 25px;
  }

  .login-main-area {
    padding: 24px 18px 30px;
  }

  .login-card-modern h2 {
    font-size: 24px;
  }

  .login-tabs-modern button {
    font-size: 15px;
  }

  .test-account-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .test-account {
    height: 35px;
  }
}
</style>

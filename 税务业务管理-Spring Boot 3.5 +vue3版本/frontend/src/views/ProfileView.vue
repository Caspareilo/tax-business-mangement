<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '新密码需为 8-64 位且同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码已修改，请使用新密码重新登录')
    formRef.value?.resetFields()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">个人设置</h1>
        <p class="page-description">查看当前账号信息与角色权限，并修改登录密码</p>
      </div>
    </div>

    <div class="section-grid">
      <div class="card">
        <div class="card-header">
          <span class="card-title">账号信息</span>
        </div>
        <div class="card-body">
          <dl class="detail-list">
            <dt>登录账号</dt>
            <dd class="mono">{{ auth.user?.username }}</dd>
            <dt>显示名称</dt>
            <dd>{{ auth.user?.displayName }}</dd>
            <dt>角色</dt>
            <dd>{{ auth.user?.roles?.join(' / ') || '-' }}</dd>
            <dt>权限数量</dt>
            <dd>{{ auth.user?.permissions?.length ?? 0 }} 项</dd>
          </dl>
          <div style="margin-top: 20px">
            <div class="form-section-title">权限清单</div>
            <div class="permission-list">
              <el-tag v-for="perm in auth.user?.permissions" :key="perm" size="small" effect="plain" type="info">{{
                perm
              }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">修改密码</span>
        </div>
        <div class="card-body">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">保存新密码</el-button>
          </el-form>
          <p class="text-muted" style="margin-top: 14px; font-size: 12px">密码需 8-64 位，且同时包含字母和数字。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

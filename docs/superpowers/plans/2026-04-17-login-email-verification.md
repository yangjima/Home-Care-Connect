# 登录注册页面邮箱验证改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将登录注册页面从手机号短信验证改为邮箱验证模式

**Architecture:** 纯前端实现，使用 Canvas 生成图形验证码，JavaScript 模拟邮箱验证码发送，所有验证逻辑在前端完成

**Tech Stack:** HTML5, CSS3, JavaScript (ES6), Canvas API

---

## 文件结构

**修改文件：**
- `prototypes/group1-user-pages/01-login.html` - 登录注册页面（HTML + CSS + JavaScript 全部在一个文件中）

---

### Task 1: 修改登录表单 HTML 结构

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:316-352`

- [ ] **Step 1: 将手机号输入框改为邮箱输入框**

找到登录表单中的手机号输入框（第 318-321 行），替换为：

```html
<div class="form-group">
    <label class="form-label">邮箱地址<span class="required">*</span></label>
    <input type="email" id="login-email" class="form-input" placeholder="请输入邮箱地址">
</div>
```

- [ ] **Step 2: 将短信验证码改为图形验证码**

找到登录表单中的验证码部分（第 329-335 行），替换为：

```html
<div class="form-group">
    <label class="form-label">图形验证码<span class="required">*</span></label>
    <div class="captcha-wrapper">
        <input type="text" id="login-captcha" class="form-input captcha-input" placeholder="请输入验证码" maxlength="4">
        <canvas id="captcha-canvas" class="captcha-canvas" width="120" height="40"></canvas>
    </div>
</div>
```

- [ ] **Step 3: 删除第三方登录区块**

找到第三方登录区块（第 339-347 行），完整删除以下内容：

```html
<!-- 删除从这里开始 -->
<div class="third-party-login">
    <div class="third-party-title">其他登录方式</div>
    <div class="third-party-btns">
        <div class="third-party-btn" title="微信登录">💚</div>
        <div class="third-party-btn" title="支付宝登录">💙</div>
        <div class="third-party-btn" title="QQ登录">💛</div>
    </div>
</div>
<!-- 删除到这里结束 -->
```

- [ ] **Step 4: 在浏览器中打开页面验证 HTML 结构**

Run: 在浏览器中打开 `prototypes/group1-user-pages/01-login.html`
Expected: 登录表单显示邮箱输入框、密码输入框、图形验证码区域（Canvas 暂时为空白）

- [ ] **Step 5: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "修改登录表单：手机号改为邮箱，短信验证码改为图形验证码，删除第三方登录"
```

---

### Task 2: 修改注册表单 HTML 结构

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:354-389`

- [ ] **Step 1: 将手机号输入框改为邮箱输入框**

找到注册表单中的手机号输入框（第 357-360 行），替换为：

```html
<div class="form-group">
    <label class="form-label">邮箱地址<span class="required">*</span></label>
    <input type="email" id="register-email" class="form-input" placeholder="请输入邮箱地址">
</div>
```

- [ ] **Step 2: 修改验证码标签和按钮文本**

找到注册表单中的验证码部分（第 361-367 行），修改为：

```html
<div class="form-group">
    <label class="form-label">邮箱验证码<span class="required">*</span></label>
    <div class="form-row">
        <input type="text" id="register-code" class="form-input" placeholder="请输入邮箱验证码" maxlength="6">
        <button type="button" id="send-code-btn" class="btn-code">获取验证码</button>
    </div>
</div>
```

- [ ] **Step 3: 为设置密码输入框添加密码强度提示**

找到设置密码输入框（第 368-371 行），在输入框后添加密码强度提示：

```html
<div class="form-group">
    <label class="form-label">设置密码<span class="required">*</span></label>
    <input type="password" id="register-password" class="form-input" placeholder="请设置6-20位登录密码">
    <div id="password-strength" class="password-strength" style="display: none;">
        <span class="strength-text">密码强度：弱</span>
    </div>
</div>
```

- [ ] **Step 4: 为确认密码输入框添加 ID**

找到确认密码输入框（第 372-375 行），添加 ID 属性：

```html
<div class="form-group">
    <label class="form-label">确认密码<span class="required">*</span></label>
    <input type="password" id="register-confirm-password" class="form-input" placeholder="请再次输入密码">
</div>
```

- [ ] **Step 5: 在浏览器中验证注册表单结构**

Run: 在浏览器中打开页面，切换到"快速注册"标签
Expected: 注册表单显示邮箱输入框、邮箱验证码输入框、设置密码（带强度提示）、确认密码

- [ ] **Step 6: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "修改注册表单：手机号改为邮箱，添加密码强度提示"
```

---

### Task 3: 添加图形验证码和密码强度提示的 CSS 样式

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:7-276`

- [ ] **Step 1: 添加图形验证码样式**

在 `<style>` 标签内，找到 `.agreement a` 样式后（第 260 行），添加图形验证码样式：

```css
.captcha-wrapper {
    display: flex;
    gap: 10px;
    align-items: center;
}
.captcha-input {
    flex: 1;
}
.captcha-canvas {
    width: 120px;
    height: 40px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.3s;
}
.captcha-canvas:hover {
    border-color: #2C7BE5;
    box-shadow: 0 2px 8px rgba(44,123,229,0.2);
}
```

- [ ] **Step 2: 添加密码强度提示样式**

在刚添加的图形验证码样式后，继续添加密码强度提示样式：

```css
.password-strength {
    margin-top: 8px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
}
.strength-text {
    font-weight: 500;
}
.strength-weak { color: #ff4d4f; }
.strength-medium { color: #faad14; }
.strength-strong { color: #52c41a; }
```

- [ ] **Step 3: 删除第三方登录相关样式**

找到并删除以下样式（第 195-237 行）：

```css
/* 删除这些样式 */
.third-party-login { ... }
.third-party-title { ... }
.third-party-title::before,
.third-party-title::after { ... }
.third-party-title::before { ... }
.third-party-title::after { ... }
.third-party-btns { ... }
.third-party-btn { ... }
.third-party-btn:hover { ... }
```

- [ ] **Step 4: 为右侧区域添加 overflow: hidden**

找到 `.login-right` 样式（第 66-72 行），添加 `overflow: hidden;`：

```css
.login-right {
    flex: 1;
    padding: 60px 50px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    overflow: hidden; /* 新增这一行 */
}
```

- [ ] **Step 5: 减小表单项间距**

找到 `.form-group` 样式（第 110-112 行），修改 margin-bottom：

```css
.form-group {
    margin-bottom: 18px; /* 从 20px 改为 18px */
}
```

- [ ] **Step 6: 在浏览器中验证样式效果**

Run: 刷新浏览器页面
Expected: 图形验证码 Canvas 有边框和悬停效果，右侧无滚动条，第三方登录样式已移除

- [ ] **Step 7: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "添加图形验证码和密码强度提示样式，删除第三方登录样式，移除右侧滚动条"
```

---

### Task 4: 实现图形验证码生成器

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:393-405`

- [ ] **Step 1: 在 script 标签中添加图形验证码生成器对象**

找到 `<script>` 标签（第 393 行），在 `switchTab` 函数之前添加：

```javascript
// 图形验证码生成器
const CaptchaGenerator = {
    code: '',
    canvas: null,
    ctx: null,
    
    init(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) return;
        this.ctx = this.canvas.getContext('2d');
        this.refresh();
        // 点击刷新
        this.canvas.addEventListener('click', () => this.refresh());
    },
    
    refresh() {
        // 生成 4 位随机字符（排除易混淆字符）
        const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
        this.code = '';
        for (let i = 0; i < 4; i++) {
            this.code += chars[Math.floor(Math.random() * chars.length)];
        }
        this.draw();
    },
    
    draw() {
        if (!this.ctx) return;
        
        // 清空画布
        this.ctx.clearRect(0, 0, 120, 40);
        
        // 绘制背景
        this.ctx.fillStyle = '#f0f0f0';
        this.ctx.fillRect(0, 0, 120, 40);
        
        // 绘制字符
        for (let i = 0; i < this.code.length; i++) {
            this.ctx.save();
            this.ctx.fillStyle = this.randomColor(50, 150);
            this.ctx.font = `${this.randomNum(20, 28)}px Arial`;
            const x = 20 + i * 25;
            const y = this.randomNum(25, 35);
            const angle = this.randomNum(-15, 15) * Math.PI / 180;
            this.ctx.translate(x, y);
            this.ctx.rotate(angle);
            this.ctx.fillText(this.code[i], 0, 0);
            this.ctx.restore();
        }
        
        // 绘制干扰线
        for (let i = 0; i < 4; i++) {
            this.ctx.strokeStyle = this.randomColor(100, 200);
            this.ctx.beginPath();
            this.ctx.moveTo(this.randomNum(0, 120), this.randomNum(0, 40));
            this.ctx.lineTo(this.randomNum(0, 120), this.randomNum(0, 40));
            this.ctx.stroke();
        }
        
        // 绘制噪点
        for (let i = 0; i < 30; i++) {
            this.ctx.fillStyle = this.randomColor(0, 255);
            this.ctx.beginPath();
            this.ctx.arc(this.randomNum(0, 120), this.randomNum(0, 40), 1, 0, 2 * Math.PI);
            this.ctx.fill();
        }
    },
    
    randomNum(min, max) {
        return Math.floor(Math.random() * (max - min) + min);
    },
    
    randomColor(min, max) {
        const r = this.randomNum(min, max);
        const g = this.randomNum(min, max);
        const b = this.randomNum(min, max);
        return `rgb(${r},${g},${b})`;
    },
    
    validate(input) {
        return input.toUpperCase() === this.code;
    }
};
```

- [ ] **Step 2: 在页面加载时初始化验证码**

在 `switchTab` 函数之后，添加页面加载事件：

```javascript
// 页面加载时初始化
window.addEventListener('DOMContentLoaded', function() {
    CaptchaGenerator.init('captcha-canvas');
});
```

- [ ] **Step 3: 在浏览器中测试验证码生成**

Run: 刷新浏览器页面
Expected: 登录表单中的 Canvas 显示 4 位随机字符验证码，点击可刷新

- [ ] **Step 4: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "实现图形验证码生成器，支持点击刷新"
```

---

### Task 5: 实现邮箱验证码管理器

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:393-405`

- [ ] **Step 1: 添加邮箱验证码管理器对象**

在 `CaptchaGenerator` 对象之后，添加邮箱验证码管理器：

```javascript
// 邮箱验证码管理器
const EmailCodeManager = {
    code: '',
    timestamp: 0,
    countdown: 0,
    timer: null,
    
    send(email, button) {
        // 验证邮箱格式
        if (!this.validateEmail(email)) {
            alert('请输入正确的邮箱地址');
            return;
        }
        
        // 生成 6 位验证码
        this.code = String(Math.floor(Math.random() * 900000) + 100000);
        this.timestamp = Date.now();
        
        // 模拟发送（控制台输出）
        console.log(`邮箱验证码：${this.code}（有效期5分钟）`);
        alert(`验证码已发送到 ${email}，请查看控制台（F12）`);
        
        // 开始倒计时
        this.startCountdown(button);
    },
    
    startCountdown(button) {
        this.countdown = 60;
        button.disabled = true;
        button.textContent = `${this.countdown}s`;
        
        this.timer = setInterval(() => {
            this.countdown--;
            if (this.countdown > 0) {
                button.textContent = `${this.countdown}s`;
            } else {
                clearInterval(this.timer);
                button.disabled = false;
                button.textContent = '获取验证码';
            }
        }, 1000);
    },
    
    validate(input) {
        // 检查验证码是否正确
        if (input !== this.code) return false;
        
        // 检查是否超时（5分钟）
        const elapsed = Date.now() - this.timestamp;
        return elapsed < 5 * 60 * 1000;
    },
    
    validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }
};
```

- [ ] **Step 2: 绑定获取验证码按钮事件**

在 `window.addEventListener('DOMContentLoaded', ...)` 中添加按钮事件绑定：

```javascript
window.addEventListener('DOMContentLoaded', function() {
    CaptchaGenerator.init('captcha-canvas');
    
    // 绑定获取验证码按钮
    const sendCodeBtn = document.getElementById('send-code-btn');
    if (sendCodeBtn) {
        sendCodeBtn.addEventListener('click', function() {
            const emailInput = document.getElementById('register-email');
            EmailCodeManager.send(emailInput.value, sendCodeBtn);
        });
    }
});
```

- [ ] **Step 3: 在浏览器中测试邮箱验证码功能**

Run: 刷新浏览器，切换到"快速注册"标签，输入邮箱后点击"获取验证码"
Expected: 弹出提示，控制台显示验证码，按钮进入 60 秒倒计时

- [ ] **Step 4: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "实现邮箱验证码管理器，支持倒计时和验证"
```

---

### Task 6: 实现密码强度检测

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:393-405`

- [ ] **Step 1: 添加密码强度检测函数**

在 `EmailCodeManager` 对象之后，添加密码强度检测函数：

```javascript
// 密码强度检测
function checkPasswordStrength(password) {
    if (password.length < 6) return { level: 'weak', text: '弱' };
    
    const hasNumber = /\d/.test(password);
    const hasLetter = /[a-zA-Z]/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    
    if (hasNumber && hasLetter && hasSpecial && password.length >= 10) {
        return { level: 'strong', text: '强' };
    } else if ((hasNumber && hasLetter) && password.length >= 8) {
        return { level: 'medium', text: '中' };
    } else {
        return { level: 'weak', text: '弱' };
    }
}
```

- [ ] **Step 2: 绑定密码输入框事件**

在 `window.addEventListener('DOMContentLoaded', ...)` 中添加密码输入框事件绑定：

```javascript
window.addEventListener('DOMContentLoaded', function() {
    CaptchaGenerator.init('captcha-canvas');
    
    // 绑定获取验证码按钮
    const sendCodeBtn = document.getElementById('send-code-btn');
    if (sendCodeBtn) {
        sendCodeBtn.addEventListener('click', function() {
            const emailInput = document.getElementById('register-email');
            EmailCodeManager.send(emailInput.value, sendCodeBtn);
        });
    }
    
    // 绑定密码强度检测
    const passwordInput = document.getElementById('register-password');
    const strengthDiv = document.getElementById('password-strength');
    if (passwordInput && strengthDiv) {
        passwordInput.addEventListener('input', function() {
            if (this.value.length > 0) {
                const result = checkPasswordStrength(this.value);
                strengthDiv.style.display = 'flex';
                strengthDiv.className = `password-strength strength-${result.level}`;
                strengthDiv.querySelector('.strength-text').textContent = `密码强度：${result.text}`;
            } else {
                strengthDiv.style.display = 'none';
            }
        });
    }
});
```

- [ ] **Step 3: 在浏览器中测试密码强度提示**

Run: 刷新浏览器，切换到"快速注册"标签，在"设置密码"框中输入不同强度的密码
Expected: 实时显示密码强度（弱/中/强），颜色随强度变化（红/橙/绿）

- [ ] **Step 4: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "实现密码强度检测，实时显示强度提示"
```

---

### Task 7: 实现登录表单验证逻辑

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:316-337`

- [ ] **Step 1: 为登录表单添加提交事件处理**

找到登录表单的 `<form>` 标签（第 317 行），添加 `id` 和阻止默认提交：

```html
<form id="login-form" onsubmit="return handleLoginSubmit(event)">
```

- [ ] **Step 2: 添加登录表单验证函数**

在 `checkPasswordStrength` 函数之后，添加登录表单验证函数：

```javascript
// 登录表单验证
function handleLoginSubmit(event) {
    event.preventDefault();
    
    const email = document.getElementById('login-email').value;
    const password = document.querySelector('#login-form input[type="password"]').value;
    const captcha = document.getElementById('login-captcha').value;
    
    // 邮箱格式验证
    if (!EmailCodeManager.validateEmail(email)) {
        alert('请输入正确的邮箱地址');
        return false;
    }
    
    // 密码非空验证
    if (!password || password.length < 6) {
        alert('密码长度至少6位');
        return false;
    }
    
    // 图形验证码验证
    if (!CaptchaGenerator.validate(captcha)) {
        alert('图形验证码错误');
        CaptchaGenerator.refresh();
        document.getElementById('login-captcha').value = '';
        return false;
    }
    
    // 验证通过
    alert('登录验证通过！（这是演示版本，实际需要调用后端接口）');
    console.log('登录信息：', { email, password });
    return false;
}
```

- [ ] **Step 3: 在浏览器中测试登录表单验证**

Run: 刷新浏览器，在登录表单中输入邮箱、密码和验证码，点击"登录"
Expected: 
- 邮箱格式错误时提示"请输入正确的邮箱地址"
- 密码少于6位时提示"密码长度至少6位"
- 验证码错误时提示"图形验证码错误"并刷新验证码
- 全部正确时提示"登录验证通过"

- [ ] **Step 4: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "实现登录表单验证逻辑"
```

---

### Task 8: 实现注册表单验证逻辑

**Files:**
- Modify: `prototypes/group1-user-pages/01-login.html:354-384`

- [ ] **Step 1: 为注册表单添加提交事件处理**

找到注册表单的 `<form>` 标签（第 356 行），添加 `id` 和阻止默认提交：

```html
<form id="register-form" onsubmit="return handleRegisterSubmit(event)">
```

- [ ] **Step 2: 添加注册表单验证函数**

在 `handleLoginSubmit` 函数之后，添加注册表单验证函数：

```javascript
// 注册表单验证
function handleRegisterSubmit(event) {
    event.preventDefault();
    
    const email = document.getElementById('register-email').value;
    const emailCode = document.getElementById('register-code').value;
    const password = document.getElementById('register-password').value;
    const confirmPassword = document.getElementById('register-confirm-password').value;
    
    // 邮箱格式验证
    if (!EmailCodeManager.validateEmail(email)) {
        alert('请输入正确的邮箱地址');
        return false;
    }
    
    // 邮箱验证码验证
    if (!EmailCodeManager.validate(emailCode)) {
        alert('邮箱验证码错误或已过期');
        return false;
    }
    
    // 密码强度验证
    if (password.length < 6 || password.length > 20) {
        alert('密码长度应为6-20位');
        return false;
    }
    
    // 确认密码验证
    if (password !== confirmPassword) {
        alert('两次输入的密码不一致');
        return false;
    }
    
    // 验证通过
    alert('注册验证通过！（这是演示版本，实际需要调用后端接口）');
    console.log('注册信息：', { email, password });
    return false;
}
```

- [ ] **Step 3: 在浏览器中测试注册表单验证**

Run: 刷新浏览器，切换到"快速注册"标签
测试步骤：
1. 输入邮箱，点击"获取验证码"，查看控制台获取验证码
2. 输入验证码、设置密码、确认密码
3. 点击"注册"按钮

Expected:
- 邮箱格式错误时提示"请输入正确的邮箱地址"
- 验证码错误时提示"邮箱验证码错误或已过期"
- 密码长度不符时提示"密码长度应为6-20位"
- 两次密码不一致时提示"两次输入的密码不一致"
- 全部正确时提示"注册验证通过"

- [ ] **Step 4: 提交修改**

```bash
git add prototypes/group1-user-pages/01-login.html
git commit -m "实现注册表单验证逻辑"
```

---

### Task 9: 最终测试和验证

**Files:**
- Test: `prototypes/group1-user-pages/01-login.html`

- [ ] **Step 1: 测试登录流程**

Run: 在浏览器中打开页面，测试完整登录流程
测试用例：
1. 输入邮箱：test@example.com
2. 输入密码：123456
3. 查看图形验证码，输入验证码
4. 点击"登录"按钮

Expected: 验证码正确时提示"登录验证通过"

- [ ] **Step 2: 测试注册流程**

Run: 切换到"快速注册"标签，测试完整注册流程
测试用例：
1. 输入邮箱：newuser@example.com
2. 点击"获取验证码"，查看控制台获取验证码
3. 输入验证码
4. 设置密码：Test@123456（测试强密码）
5. 确认密码：Test@123456
6. 点击"注册"按钮

Expected: 
- 密码强度显示"强"（绿色）
- 提示"注册验证通过"

- [ ] **Step 3: 测试图形验证码刷新**

Run: 在登录表单中，多次点击图形验证码图片
Expected: 每次点击都生成新的验证码

- [ ] **Step 4: 测试邮箱验证码倒计时**

Run: 在注册表单中，点击"获取验证码"按钮
Expected: 按钮显示 60s 倒计时，倒计时期间按钮禁用，倒计时结束后恢复

- [ ] **Step 5: 测试密码强度提示**

Run: 在注册表单的"设置密码"框中依次输入：
- "123456"（弱）
- "abc12345"（中）
- "Abc@12345"（强）

Expected: 密码强度提示实时更新，颜色分别为红、橙、绿

- [ ] **Step 6: 验证右侧无滚动条**

Run: 调整浏览器窗口大小
Expected: 右侧登录/注册区域始终无滚动条，内容完整显示

- [ ] **Step 7: 验证第三方登录已移除**

Run: 检查登录表单
Expected: 不再显示"其他登录方式"区块

- [ ] **Step 8: 最终提交**

```bash
git add -A
git commit -m "完成登录注册页面邮箱验证改造，所有功能测试通过"
```

---

## 实现完成

所有任务已完成，页面功能包括：

✅ 登录表单：邮箱 + 密码 + 图形验证码
✅ 注册表单：邮箱 + 邮箱验证码 + 设置密码 + 确认密码
✅ 图形验证码：Canvas 生成，点击刷新
✅ 邮箱验证码：模拟发送，60秒倒计时
✅ 密码强度提示：实时显示弱/中/强
✅ 表单验证：完整的前端验证逻辑
✅ UI 优化：移除第三方登录，移除滚动条

---

**注意事项：**
- 这是原型演示版本，所有验证逻辑在前端实现
- 生产环境需要后端接口支持
- 图形验证码和邮箱验证码需要后端生成和验证
- 密码需要后端加密存储


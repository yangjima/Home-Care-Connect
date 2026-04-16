# 登录注册页面邮箱验证改造设计文档

**日期：** 2026-04-17  
**文件：** `prototypes/group1-user-pages/01-login.html`  
**目标：** 将登录注册从手机号短信验证改为邮箱验证模式

---

## 一、需求概述

### 当前状态
- 登录：手机号 + 密码 + 短信验证码
- 注册：手机号 + 短信验证码 + 设置密码
- 包含第三方登录（微信、支付宝、QQ）
- 右侧区域有滚动条

### 目标状态
- 登录：邮箱 + 密码 + 图形验证码
- 注册：邮箱 + 邮箱验证码 + 设置密码
- 移除第三方登录方式
- 右侧区域无滚动条

---

## 二、技术方案

### 实现方式
**纯前端实现**，适用于原型演示阶段：
- 图形验证码：Canvas 绘制，前端生成和校验
- 邮箱验证码：模拟发送，控制台输出用于测试
- 无需后端支持，可直接在浏览器中演示

---

## 三、登录表单设计

### 表单结构
```
邮箱地址 [必填]
  └─ <input type="email">
  └─ 前端验证邮箱格式

密码 [必填]
  └─ <input type="password">
  └─ 保留"忘记密码"链接

图形验证码 [必填]
  └─ <input type="text" maxlength="4">
  └─ <canvas> 显示验证码图片
  └─ 点击图片刷新验证码
```

### 图形验证码实现细节

**Canvas 规格：**
- 尺寸：120x40 像素
- 字符数：4 位（数字 + 大写字母混合）
- 字符集：0-9, A-Z（排除易混淆字符 0/O, 1/I）

**视觉效果：**
- 随机字体大小：20-28px
- 随机旋转角度：-15° 到 +15°
- 随机颜色：深色系（确保可读性）
- 干扰线：3-5 条随机曲线
- 噪点：20-30 个随机点

**交互逻辑：**
- 页面加载时自动生成验证码
- 点击 Canvas 图片刷新验证码
- 输入框失焦时验证（不区分大小写）
- 验证失败自动刷新验证码

### 移除内容
- 删除"其他登录方式"整个区块（HTML + CSS）
- 删除第三方登录相关样式（`.third-party-login`, `.third-party-btn` 等）

---

## 四、注册表单设计

### 表单结构
```
邮箱地址 [必填]
  └─ <input type="email">
  └─ 前端验证邮箱格式

邮箱验证码 [必填]
  └─ <input type="text" maxlength="6">
  └─ <button> 获取验证码（60秒倒计时）

设置密码 [必填]
  └─ <input type="password">
  └─ 密码强度提示（弱/中/强）
  └─ 要求：6-20位

确认密码 [必填]
  └─ <input type="password">
  └─ 实时校验是否与设置密码一致

分销码 [选填]
  └─ <input type="text">
  └─ 保持原有逻辑不变
```

### 邮箱验证码逻辑

**获取验证码流程：**
1. 点击按钮前验证邮箱格式
2. 格式正确：生成 6 位随机数字验证码
3. 控制台输出验证码（模拟发送）：`console.log('邮箱验证码：123456')`
4. 按钮进入倒计时状态：`60s` → `59s` → ... → `0s`
5. 倒计时期间按钮禁用（灰色样式）
6. 倒计时结束恢复"获取验证码"文本，可重新点击

**前端验证：**
- 提交表单时检查验证码是否与生成的一致
- 验证码有效期：5 分钟（前端计时）
- 超时需重新获取

### 密码强度提示

**强度判断规则：**
- 弱（红色）：纯数字或纯字母，长度 < 8
- 中（橙色）：数字 + 字母组合，长度 ≥ 8
- 强（绿色）：数字 + 字母 + 特殊字符，长度 ≥ 10

**显示位置：**
- 在"设置密码"输入框下方
- 实时更新（input 事件监听）
- 样式：小字号，颜色根据强度变化

---

## 五、UI 调整

### 移除滚动条
```css
.login-right {
    overflow: hidden; /* 新增 */
    /* 保持原有样式 */
}
```

**内容适配策略：**
- 减小表单项间距：`margin-bottom: 20px` → `18px`
- 减小按钮上下内边距：`padding: 14px` → `padding: 12px`
- 如仍超出，减小整体 `padding: 60px 50px` → `padding: 50px 40px`

### 图形验证码样式
```css
.captcha-wrapper {
    display: flex;
    gap: 10px;
    align-items: center;
}

.captcha-input {
    flex: 1;
    /* 继承 .form-input 样式 */
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

### 密码强度提示样式
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

---

## 六、JavaScript 实现

### 图形验证码模块
```javascript
// 验证码生成器
const CaptchaGenerator = {
    code: '',
    canvas: null,
    ctx: null,
    
    init(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.refresh();
    },
    
    refresh() {
        // 生成 4 位随机字符
        const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
        this.code = '';
        for (let i = 0; i < 4; i++) {
            this.code += chars[Math.floor(Math.random() * chars.length)];
        }
        this.draw();
    },
    
    draw() {
        // 清空画布
        this.ctx.clearRect(0, 0, 120, 40);
        
        // 绘制背景
        this.ctx.fillStyle = '#f0f0f0';
        this.ctx.fillRect(0, 0, 120, 40);
        
        // 绘制字符
        for (let i = 0; i < this.code.length; i++) {
            this.ctx.save();
            // 随机颜色、位置、旋转
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

### 邮箱验证码模块
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
        alert(`验证码已发送到 ${email}，请查看控制台`);
        
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

### 密码强度检测
```javascript
// 密码强度检测器
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

// 实时更新密码强度提示
function updatePasswordStrength(input, strengthElement) {
    const result = checkPasswordStrength(input.value);
    strengthElement.className = `password-strength strength-${result.level}`;
    strengthElement.querySelector('.strength-text').textContent = `密码强度：${result.text}`;
}
```

---

## 七、表单验证逻辑

### 登录表单验证
```javascript
function validateLoginForm(formData) {
    const { email, password, captcha } = formData;
    
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
        return false;
    }
    
    return true;
}
```

### 注册表单验证
```javascript
function validateRegisterForm(formData) {
    const { email, emailCode, password, confirmPassword } = formData;
    
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
    
    return true;
}
```

---

## 八、实现步骤

### 步骤 1：修改 HTML 结构
1. 登录表单：手机号 → 邮箱，短信验证码 → 图形验证码
2. 注册表单：手机号 → 邮箱，短信验证码 → 邮箱验证码
3. 删除第三方登录区块

### 步骤 2：调整 CSS 样式
1. 添加图形验证码样式（`.captcha-wrapper`, `.captcha-canvas`）
2. 添加密码强度提示样式（`.password-strength`）
3. 移除第三方登录相关样式
4. 为 `.login-right` 添加 `overflow: hidden`

### 步骤 3：实现 JavaScript 功能
1. 实现图形验证码生成器（`CaptchaGenerator`）
2. 实现邮箱验证码管理器（`EmailCodeManager`）
3. 实现密码强度检测（`checkPasswordStrength`）
4. 绑定表单提交事件和验证逻辑

### 步骤 4：测试验证
1. 测试图形验证码生成和刷新
2. 测试邮箱验证码倒计时
3. 测试密码强度实时提示
4. 测试表单提交验证逻辑
5. 验证右侧区域无滚动条

---

## 九、注意事项

### 安全性说明
- 此为**原型演示版本**，验证逻辑在前端实现
- 生产环境必须使用后端验证：
  - 图形验证码：后端生成，Session 存储
  - 邮箱验证码：后端发送真实邮件，数据库存储
  - 密码：后端加密存储（bcrypt/scrypt）

### 用户体验优化
- 邮箱输入框失焦时自动验证格式，提前提示错误
- 图形验证码错误时自动刷新，无需手动点击
- 密码强度实时反馈，引导用户设置强密码
- 倒计时期间禁用按钮，防止重复点击

### 兼容性
- Canvas API：支持所有现代浏览器
- 邮箱验证正则：标准 RFC 5322 简化版
- CSS 样式：使用 Flexbox 布局，IE11+ 支持

---

## 十、后续扩展

### 可选功能（暂不实现）
- 记住登录状态（localStorage）
- 邮箱自动补全（常见邮箱后缀）
- 密码可见性切换（眼睛图标）
- 表单字段实时验证提示（红色边框 + 错误文本）

### 对接后端时需修改
- 图形验证码：调用 `/api/captcha/generate` 和 `/api/captcha/verify`
- 邮箱验证码：调用 `/api/email/send-code` 和 `/api/email/verify-code`
- 登录接口：`POST /api/auth/login`
- 注册接口：`POST /api/auth/register`

---

**文档结束**

<template>
  <footer class="app-footer">
    <div class="container">
      <div class="footer-grid">
        <div class="footer-brand">
          <h3>居服通</h3>
          <p>智慧社区服务管理平台，为您的社区生活提供全方位服务支持。</p>
        </div>

        <div class="footer-links">
          <h4>快速链接</h4>
          <router-link to="/properties">找房源</router-link>
          <router-link to="/services">找服务</router-link>
          <router-link to="/secondhand">二手交易</router-link>
          <router-link to="/ai">AI 助手</router-link>
        </div>

        <div class="footer-links">
          <h4>帮助中心</h4>
          <a href="#">常见问题</a>
          <a href="#">服务协议</a>
          <a href="#">隐私政策</a>
          <a href="#">联系我们</a>
        </div>

        <div class="footer-contact">
          <h4>联系方式</h4>
          <p>📧 15194479717@163.com</p>
          <p>📞 15194479717</p>
          <p>📍 上海浦东科创园</p>
        </div>
      </div>

      <div class="footer-bottom">
        <p>© 2026 居服通 Home Care Connect. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.app-footer {
  background: var(--color-text-primary);
  color: #ccc;
  padding: var(--spacing-xxl) 0 var(--spacing-lg);
  margin-top: auto;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.5fr;
  gap: var(--spacing-xl);
  padding-bottom: var(--spacing-xl);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-brand {
  h3 {
    font-size: 24px;
    color: white;
    margin-bottom: var(--spacing-md);
  }

  p {
    font-size: 14px;
    line-height: 1.8;
  }
}

.footer-links {
  h4 {
    color: white;
    font-size: 15px;
    margin-bottom: var(--spacing-md);
  }

  a {
    display: block;
    color: #aaa;
    font-size: 14px;
    margin-bottom: var(--spacing-sm);
    transition: color 0.2s;

    &:hover {
      color: var(--color-primary);
    }
  }
}

.footer-contact {
  h4 {
    color: white;
    font-size: 15px;
    margin-bottom: var(--spacing-md);
  }

  p {
    font-size: 14px;
    margin-bottom: var(--spacing-sm);
    color: #aaa;
  }
}

.footer-bottom {
  padding-top: var(--spacing-lg);
  text-align: center;
  font-size: 13px;
  color: #777;
}

@media (max-width: 768px) {
  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-lg);
  }

  .footer-brand {
    grid-column: 1 / -1;
  }
}
</style>

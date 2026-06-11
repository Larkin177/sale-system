import os
os.chdir('E:/DevEnv/project/sale-system')
with open('frontend/src/views/customer/Home.vue', 'r', encoding='utf-8') as f:
    c = f.read()

# ===== Hero CSS =====
# Fix hero padding
old = '.hero {\n  background: linear-gradient(135deg, var(--hero-bg) 0%, var(--hero-bg-end) 100%);\n  padding: 120px 24px 80px;\n  text-align: center;\n  position: relative;\n  overflow: hidden;\n}'
new = '.hero {\n  background: linear-gradient(135deg, var(--hero-bg) 0%, var(--hero-bg-end) 100%);\n  padding: 0 24px 100px;\n  text-align: center;\n  position: relative;\n  overflow: hidden;\n  min-height: 480px;\n  display: flex;\n  flex-direction: column;\n}'
c = c.replace(old, new)
print("hero CSS updated")

# hero-content flex center
old = '.hero-content {\n  position: relative;\n  z-index: 1;\n  max-width: 700px;\n  margin: 0 auto;\n}'
new = '.hero-content {\n  position: relative;\n  z-index: 1;\n  max-width: 800px;\n  margin: 0 auto;\n  flex: 1;\n  display: flex;\n  flex-direction: column;\n  justify-content: center;\n}'
c = c.replace(old, new)
print("hero-content updated")

# Bigger hero title
old = '.hero-title {\n  font-size: 52px;\n  font-weight: 700;\n  color: white;\n  margin: 0 0 20px;\n  letter-spacing: -0.5px;\n  line-height: 1.2;\n}'
new = '.hero-title {\n  font-size: 56px;\n  font-weight: 800;\n  color: white;\n  margin: 0 0 16px;\n  letter-spacing: -0.5px;\n  line-height: 1.15;\n}'
c = c.replace(old, new)
print("hero-title updated")

# Bigger subtitle
old = '.hero-subtitle {\n  font-size: 20px;\n  color: rgba(255, 255, 255, 0.9);\n  margin: 0 0 40px;\n  font-weight: 300;\n}'
new = '.hero-subtitle {\n  font-size: 22px;\n  color: rgba(255, 255, 255, 0.9);\n  margin: 0 0 44px;\n  font-weight: 400;\n}'
c = c.replace(old, new)
print("hero-subtitle updated")

# Bigger hero btn
old = '.hero-btn {\n  padding: 14px 48px;\n  font-size: 16px;\n  border-radius: 8px;\n  font-weight: 500;\n  transition: all 0.3s ease;\n  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);\n}'
new = '.hero-btn {\n  padding: 16px 56px;\n  font-size: 18px;\n  border-radius: 10px;\n  font-weight: 600;\n  transition: all 0.3s ease;\n  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);\n}'
c = c.replace(old, new)
print("hero-btn updated")

# ===== Replace top-nav CSS =====
old = "/* ==================== Top Nav ==================== */\n.top-nav {\n  background: white;\n  border-bottom: 1px solid #eef0f6;\n  position: sticky;\n  top: 0;\n  z-index: 100;\n}\n\n.nav-inner {\n  max-width: 1200px;\n  margin: 0 auto;\n  padding: 0 24px;\n  height: 56px;\n  display: flex;\n  align-items: center;\n  justify-content: space-between;\n}\n\n.nav-brand {\n  font-size: 18px;\n  font-weight: 700;\n  background: linear-gradient(135deg, #667eea, #764ba2);\n  -webkit-background-clip: text;\n  -webkit-text-fill-color: transparent;\n  background-clip: text;\n}\n\n.nav-links {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n}"
new = "/* ==================== Hero Nav (merged) ==================== */\n.hero-nav {\n  width: 100%;\n  max-width: 1200px;\n  margin: 0 auto;\n  padding: 16px 24px 50px;\n  display: flex;\n  align-items: center;\n  justify-content: space-between;\n  position: relative;\n  z-index: 2;\n}\n\n.hero-brand {\n  font-size: 20px;\n  font-weight: 800;\n  color: #fff;\n  text-shadow: 0 2px 8px rgba(0,0,0,0.15);\n  letter-spacing: 0.5px;\n}\n\n.hero-orders-btn {\n  color: rgba(255,255,255,0.75) !important;\n  font-size: 13px !important;\n  font-weight: 400 !important;\n  background: transparent !important;\n  border: none !important;\n  padding: 0 !important;\n  transition: color 0.25s !important;\n  letter-spacing: 1px;\n  position: relative;\n}\n.hero-orders-btn::after {\n  content: '';\n  position: absolute;\n  bottom: -2px;\n  left: 0;\n  width: 0;\n  height: 1px;\n  background: rgba(255,255,255,0.5);\n  transition: width 0.3s ease;\n}\n.hero-orders-btn:hover {\n  color: #fff !important;\n  background: transparent !important;\n}\n.hero-orders-btn:hover::after { width: 100%; }"
c = c.replace(old, new)
print("nav CSS replaced")

# ===== Fix tabs padding =====
c = c.replace(
    '.main-tabs :deep(.el-tabs__content) { padding: 40px 32px; }',
    '.main-tabs :deep(.el-tabs__content) { padding: 0; }'
)
print("tabs padding fixed")

# ===== tab-content flex center =====
old = '.tab-content {\n  min-height: 200px;\n}'
new = '.tab-content {\n  min-height: 280px;\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  padding: 32px;\n}'
c = c.replace(old, new)
print("tab-content updated")

# ===== Bigger feature cards =====
old = '.feature-card-inline {\n  background: #f8faff;\n  border: 2px solid #eef0f6;\n  border-radius: 16px;\n  padding: 32px 24px;\n  text-align: center;\n  transition: all 0.3s ease;\n}'
new = '.feature-card-inline {\n  background: #f8faff;\n  border: 2px solid #eef0f6;\n  border-radius: 16px;\n  padding: 40px 28px;\n  text-align: center;\n  transition: all 0.3s ease;\n}'
c = c.replace(old, new)
print("feature cards updated")

old = '.feature-title-inline {\n  font-size: 18px;\n  font-weight: 700;\n  color: #1f2937;\n  margin: 0 0 10px;\n}'
new = '.feature-title-inline {\n  font-size: 20px;\n  font-weight: 700;\n  color: #1f2937;\n  margin: 0 0 12px;\n}'
c = c.replace(old, new)
print("feature title updated")

old = '.feature-desc-inline {\n  font-size: 14px;\n  color: #6b7280;\n  margin: 0;\n  line-height: 1.6;\n}'
new = '.feature-desc-inline {\n  font-size: 15px;\n  color: #6b7280;\n  margin: 0;\n  line-height: 1.7;\n}'
c = c.replace(old, new)
print("feature desc updated")

# ===== Bigger tutorial card =====
old = '.tutorial-card {\n  background: white;\n  border-radius: 14px;\n  overflow: hidden;\n  border: 1px solid #f3f4f6;\n  cursor: pointer;\n  transition: all 0.25s;\n}'
new = '.tutorial-card {\n  display: flex;\n  flex-direction: column;\n  background: #f8faff;\n  border: 2px solid #eef0f6;\n  border-radius: 16px;\n  overflow: hidden;\n  cursor: pointer;\n  transition: all 0.3s ease;\n  min-height: 240px;\n}'
c = c.replace(old, new)
print("tutorial card updated")

old = '.tutorial-card:hover {\n  transform: translateY(-4px);\n  box-shadow: 0 8px 24px rgba(0,0,0,0.1);\n}'
new = '.tutorial-card:hover {\n  transform: translateY(-4px);\n  box-shadow: 0 8px 24px rgba(102,126,234,0.1);\n  border-color: #e0e7ff;\n}'
c = c.replace(old, new)
print("tutorial card hover updated")

old = '.tutorial-thumb {\n  height: 140px;\n  background: linear-gradient(135deg, #eef0f6 0%, #e8ecf4 100%);\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  position: relative;\n  overflow: hidden;\n  flex-shrink: 0;\n}'
new = '.tutorial-thumb {\n  height: 170px;\n  background: linear-gradient(135deg, #eef0f6 0%, #e8ecf4 100%);\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  position: relative;\n  overflow: hidden;\n  flex-shrink: 0;\n}'
c = c.replace(old, new)
print("tutorial thumb updated")

old = '.tutorial-info {\n  padding: 14px 18px;\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  flex: 1;\n}'
new = '.tutorial-info {\n  padding: 18px 20px;\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  flex: 1;\n}'
c = c.replace(old, new)
print("tutorial info updated")

old = '.tutorial-info h4 {\n  margin: 0;\n  font-size: 15px;\n  font-weight: 600;\n  color: #1f2937;\n  flex: 1;\n  overflow: hidden;\n  text-overflow: ellipsis;\n  white-space: nowrap;\n}'
new = '.tutorial-info h4 {\n  margin: 0;\n  font-size: 16px;\n  font-weight: 600;\n  color: #1f2937;\n  flex: 1;\n  overflow: hidden;\n  text-overflow: ellipsis;\n  white-space: nowrap;\n}'
c = c.replace(old, new)
print("tutorial h4 updated")

# ===== Grid gaps =====
old = '.features-grid {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 20px;\n}'
new = '.features-grid {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 24px;\n}'
c = c.replace(old, new)
print("features grid gap updated")

old = '.tutorials-grid {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 20px;\n}'
new = '.tutorials-grid {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 24px;\n}'
c = c.replace(old, new)
print("tutorials grid gap updated")

# ===== Other =====
c = c.replace('.features-area { margin-bottom: 16px; }', '.features-area { width: 100%; }')
c = c.replace('.tutorials-area { margin-bottom: 16px; }', '.tutorials-area { width: 100%; }')
c = c.replace('.tutorials-empty { padding: 48px 0; }', '.tutorials-empty { padding: 8px 0; }')
c = c.replace('.hero-skeleton { min-height: 300px; }', '.hero-skeleton { min-height: 480px; }')
print("other CSS updated")

with open('frontend/src/views/customer/Home.vue', 'w', encoding='utf-8') as f:
    f.write(c)
print("ALL CSS CHANGES DONE")

<template>
  <div class="base-card" :class="{ 'base-card--clickable': clickable }" @click="onClick">
    <slot name="media" />
    <div v-if="$slots.default" class="base-card__body">
      <slot />
    </div>
    <slot name="footer" />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  clickable?: boolean
}>()

const emit = defineEmits<{
  (e: 'click', ev: MouseEvent): void
}>()

function onClick(ev: MouseEvent) {
  if (props.clickable) emit('click', ev)
}
</script>

<style scoped lang="scss">
.base-card {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;

  &--clickable {
    cursor: pointer;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-base);
  }
}

.base-card__body {
  padding: var(--spacing-md);
}
</style>

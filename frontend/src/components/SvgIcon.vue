<script setup>
import { computed } from 'vue'
import { I, I1024, svg24Attrs, svg1024Attrs } from '../icons.js'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 }
})

const attrs = computed(() => I1024.has(props.name) ? svg1024Attrs : svg24Attrs)
const paths = computed(() => I[props.name] || '')
</script>

<template>
  <svg v-bind="attrs" v-html="paths" :width="size" :height="size"></svg>
</template>

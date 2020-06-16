<template>
	<div>
		<button type="primary" @click="checkPermission">权限</button>
		<button type="primary" @click="gotoNativePage">摄像头推流</button>
		<button type="primary" @click="onClickInit">初始化</button>
		<button type="primary" @click="onClickPush">推流</button>
		<button type="primary" @click="onClickStop">停止</button>
	</div>
</template>

<script>
	// 获取 module
	let testModule = uni.requireNativePlugin("Mrtan-Qiniu")
	let permissionModule = uni.requireNativePlugin("Mrtan-Permission")
	let modal = uni.requireNativePlugin('modal');
	const pushUrl = "rtmp://push.dwusoft.com/2e0dd3989fc540c19a3d5218742377af/2e0dd3989fc540c19a3d5218742377af?auth_key=1592355448-0-0-214b3dad75104daf7bf3bcfb4f3bce4f&itemId=2e0dd3989fc540c19a3d5218742377af"
		export default {
			data() {
			    return {  
			    }  
			}, 
			onLoad() {
				plus.globalEvent.addEventListener('TestEvent', function(e) {
					modal.toast({
						message: "TestEvent收到：" + e.msg,
						duration: 1.5
					});
				});
			},
			methods: {
				checkPermission() {
					permissionModule.checkPush((ret) => {
							modal.toast({
								message: ret,
								duration: 1.5
							});
						});
				},
				gotoNativePage() {
					testModule.gotoNativePage({"url":pushUrl});
				},
				onClickInit() {
					testModule.setStreamingStateChangedListener((ret) => {
						modal.toast({
							message: ret,
							duration: 1.5
						});
					});
					testModule.setShutterStateCallback((ret) => {
						modal.toast({
							message: ret,
							duration: 1.5
						});
					});
					//开始直播
					testModule.init({
						"url": pushUrl
					});
				},
				onClickPush() {
					//推流
					testModule.startStream()
				},
				onClickStop() {
					//停止推流
					testModule.stopStream()
				}
			}
		}
	</script>
	

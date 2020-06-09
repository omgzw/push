<template>
	<view>
		<camera device-position="back" flash="off" @error="error" style="width: 100%; height: 300px;"></camera>
		<button type="primary" @click="takePhoto">拍照</button>
		<view>预览</view>
		<image mode="widthFix" :src="src"></image>
	</view>
	<!-- <div>
		<button type="primary" @click="testAsyncFunc">testAsyncFunc</button>
		<button type="primary" @click="checkPermission">权限</button>
		<button type="primary" @click="onClickInit">初始化</button>
		<button type="primary" @click="onClickPush">推流</button>
		<button type="primary" @click="onClickStop">停止</button>
	</div> -->
</template>

<script>
	// 获取 module
	let testModule = uni.requireNativePlugin("Mrtan-Qiniu")
	let permissionModule = uni.requireNativePlugin("Mrtan-Permission")
	let modal = uni.requireNativePlugin('modal');
	export default {
		data() {
			return {
				src: ""
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
			testAsyncFunc() {
				modal.toast({
					message: "TestEvent收到：",
					duration: 1.5
				});
			},
			checkPermission() {
				permissionModule.checkMicrophone((ret) => {
					modal.toast({
						message: ret,
						duration: 1.5
					});
				});
			},
			gotoNativePage() {
				testModule.gotoNativePage();
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
					"url": 'rtmp://push.dwusoft.com/ea2e27a612e24f7f938f04a3e843dbce/ea2e27a612e24f7f938f04a3e843dbce?auth_key=1591718495-0-0-f0806fc5159a61ecbe627a83a89efc55&itemId=ea2e27a612e24f7f938f04a3e843dbce'
				});
			},
			onClickPush() {
				//推流
				testModule.startStream()
			},
			onClickStop() {
				//停止推流
				testModule.stopStream()
			},
			takePhoto() {
				const ctx = uni.createCameraContext();
				ctx.takePhoto({
					quality: 'high',
					success: (res) => {
						this.src = res.tempImagePath
					}
				});
			},
			error(e) {
				console.log(e.detail);
			}
		}
	}
</script>

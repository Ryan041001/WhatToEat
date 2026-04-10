import { WechatLogin } from '../../api/auth';

const app = getApp();

function pickToken(payload) {
	if (!payload) {
		return '';
	}

	if (typeof payload === 'string') {
		return payload;
	}

	const data = payload.data || payload;
	return data.token || data.accessToken || '';
}

function pickUser(payload) {
	if (!payload) {
		return null;
	}

	const data = payload.data || payload;
	return data.user || data.profile || null;
}

Page({
	data: {
		loading: false,
		usingMockLogin: false,
		loginError: ''
	},

	onShow() {
		const user = app.getCurrentUser();
		if (user) {
			wx.redirectTo({
				url: '/pages/home/home'
			});
		}
	},

	async handleWechatLogin() {
		if (this.data.loading) {
			return;
		}

		this.setData({ loading: true, loginError: '', usingMockLogin: false });

		try {
			const loginRes = await wxLoginPromise();
			const code = loginRes.code;
			if (!code) {
				throw new Error('未获取到微信登录code');
			}

			const response = await WechatLogin(code);
			const token = pickToken(response);
			const user = pickUser(response) || { nickname: '微信用户' };

			if (!token) {
				throw new Error('后端未返回有效token');
			}

			app.setAuth(token, user);
			await app.bootstrapRestaurants({ force: true });

			wx.showToast({ title: '登录成功', icon: 'success' });
			wx.redirectTo({ url: '/pages/home/home' });
		} catch (error) {
			// API 不可用时，走本地 Mock 会话，保证开发流程可继续。
			app.createMockSession();
			await app.bootstrapRestaurants({ force: true });
			this.setData({ usingMockLogin: true, loginError: error.message || '登录失败' });

			wx.showToast({
				title: '已切换 Mock 登录',
				icon: 'none'
			});
			wx.redirectTo({ url: '/pages/home/home' });
		} finally {
			this.setData({ loading: false });
		}
	}
});

function wxLoginPromise() {
	return new Promise((resolve, reject) => {
		wx.login({
			success: resolve,
			fail: reject
		});
	});
}

import { WechatLogin } from '../../api/auth';
import { DEFAULT_AVATAR_URL } from '../../utils/restaurant-state';

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
		loginError: '',
		nickname: '',
		avatarUrl: '',
		hasCustomAvatar: false
	},

	onShow() {
		const user = app.getCurrentUser();
		if (user) {
			wx.redirectTo({
				url: '/pages/home/home'
			});
		}
	},

	handleNicknameInput(e) {
		this.setData({
			nickname: (e && e.detail ? e.detail.value : '') || '',
			loginError: ''
		});
	},

	handleChooseAvatar(e) {
		const avatarUrl = e && e.detail && e.detail.avatarUrl ? e.detail.avatarUrl : '';
		this.setData({
			avatarUrl,
			hasCustomAvatar: !!(e && e.detail && e.detail.avatarUrl),
			loginError: ''
		});
	},

	async handleWechatLogin() {
		if (this.data.loading) {
			return;
		}

		const nickname = (this.data.nickname || '').trim();
		if (!nickname) {
			this.setData({ loginError: '请先填写昵称' });
			wx.showToast({
				title: '请先填写昵称',
				icon: 'none'
			});
			return;
		}

		this.setData({ loading: true, loginError: '' });

		try {
			const loginRes = await wxLoginPromise();
			const code = loginRes.code;
			if (!code) {
				throw new Error('未获取到微信登录code');
			}

			const avatarUrl = this.data.hasCustomAvatar && this.data.avatarUrl
				? this.data.avatarUrl
				: DEFAULT_AVATAR_URL;
			const response = await WechatLogin(code, nickname, avatarUrl);
			const token = pickToken(response);
			const user = pickUser(response) || { nickname, avatarUrl };

			if (!token) {
				throw new Error('后端未返回有效token');
			}

			app.setAuth(token, {
				...user,
				nickname: user.nickname || nickname,
				avatarUrl: user.avatarUrl || avatarUrl
			});
			try {
				await app.bootstrapRestaurants({ force: true });
			} catch (bootstrapError) {}

			wx.showToast({ title: '登录成功', icon: 'success' });
			wx.redirectTo({ url: '/pages/home/home' });
		} catch (error) {
			app.clearAuth();
			this.setData({ loginError: error.message || '登录失败，请检查后端服务和网络连接' });
			wx.showToast({
				title: '登录失败',
				icon: 'none'
			});
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

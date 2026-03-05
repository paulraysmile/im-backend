package com.bx.implatform.service;

import com.bx.implatform.vo.LoginVO;
import com.bx.implatform.vo.QrLoginStatusVO;
import com.bx.implatform.vo.QrLoginVO;

public interface QrLoginService {

    /**
     * 生成扫码登录二维码
     * @return 二维码信息
     */
    QrLoginVO generateQrCode();

    /**
     * 查询扫码登录状态
     * @param qrCode 二维码标识
     * @return 登录状态
     */
    QrLoginStatusVO getLoginStatus(String qrCode);

    /**
     * 扫描二维码
     * @param qrCode 二维码标识
     */
    void scanQrCode(String qrCode);

    /**
     * 确认扫码登录
     * @param qrCode 二维码标识
     * @return 登录信息
     */
    LoginVO confirmQrLogin(String qrCode);

    /**
     * 取消扫码登录
     * @param qrCode 二维码标识
     */
    void cancelQrLogin(String qrCode);

}

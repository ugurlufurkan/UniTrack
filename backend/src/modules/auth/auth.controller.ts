import { Request, Response } from "express";
import { authService } from "./auth.service";

class AuthController {
  async googleLogin(req: Request, res: Response) {
    const { idToken } = req.body;

    const result = await authService.googleLogin(idToken, req);

    return res.status(200).json(result);
  }

  async emailPasswordLogin(req: Request, res: Response) {
    const { email, password } = req.body;

    const result = await authService.emailPasswordLogin(email, password, req);

    return res.status(200).json(result);
  }

  async register(req: Request, res: Response) {
    const { email, password, name } = req.body;

    const result = await authService.register({ email, password, name }, req);

    return res.status(200).json(result);
  }

  async refresh(req: Request, res: Response) {
    const { refreshToken } = req.body;

    const result = await authService.refresh(refreshToken, req);

    return res.status(200).json(result);
  }

  async logout(req: Request, res: Response) {
    const { refreshToken } = req.body;

    const result = await authService.logout(refreshToken, req);

    return res.status(200).json(result);
  }

  async logoutAll(req: Request, res: Response) {
    const userId = req.user!.userId;

    const result = await authService.logoutAll(userId, req);

    return res.status(200).json(result);
  }

  async logoutDevice(req: Request, res: Response) {
    const userId = req.user!.userId;
    const tokenId = req.params.tokenId as string;

    const result = await authService.logoutDevice(userId, tokenId, req);

    return res.status(200).json(result);
  }

  async getDevices(req: Request, res: Response) {
    const userId = req.user!.userId;

    const result = await authService.getDevices(userId);

    return res.status(200).json(result);
  }

  async requestEmailVerification(req: Request, res: Response) {
    const userId = req.user!.userId;

    const result = await authService.requestEmailVerification(userId);

    return res.status(200).json(result);
  }

  async verifyEmail(req: Request, res: Response) {
    const { token } = req.body;

    const result = await authService.verifyEmail(token);

    return res.status(200).json(result);
  }

  async forgotPassword(req: Request, res: Response) {
    const { email } = req.body;

    const result = await authService.forgotPassword(email);

    return res.status(200).json(result);
  }

  async resetPassword(req: Request, res: Response) {
    const { token, newPassword } = req.body;

    const result = await authService.resetPassword(token, newPassword);

    return res.status(200).json(result);
  }

  async changePassword(req: Request, res: Response) {
    const userId = req.user!.userId;
    const { currentPassword, newPassword } = req.body;

    const result = await authService.changePassword(userId, currentPassword, newPassword);

    return res.status(200).json(result);
  }

  async me(req: Request, res: Response) {
    return res.status(200).json({
      success: true,
      user: req.user,
    });
  }

  async deleteAccount(req: Request, res: Response) {
    const userId = req.user!.userId;

    await authService.deleteAccount(userId, req);

    return res.status(204).send();
  }
}

export const authController = new AuthController();
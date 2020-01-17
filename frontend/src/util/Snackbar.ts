export declare interface ISnackbar {
  /**
   * Displays an error message in the snackbar.
   *
   * @param {string} error the error message
   * @memberof ISnackbar
   */
  setError(error: string): void

  /**
   * Displays a success message in the snackbar
   *
   * @param {string} message the message
   * @memberof ISnackbar
   */
  setSuccess(message: string): void

  /**
   * Displays a loading indicator in the snack bar.
   *
   * @memberof ISnackbar
   */
  setLoading(): void

  /**
   * Marks loading as completed, hiding it again and flashing a success text
   *
   * @memberof ISnackbar
   */
  finishedLoading(): void
}

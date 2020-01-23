export declare interface ISnackbar {
  /**
   * Displays an error message in the snackbar.
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {string} error the error message
   * @memberof ISnackbar
   */
  setError(tag: string, error: string): void

  /**
   * Displays a success message in the snackbar
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {string} message the message
   * @memberof ISnackbar
   */
  setSuccess(tag: string, message: string): void

  /**
   * Displays a loading indicator in the snack bar.
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @memberof ISnackbar
   */
  setLoading(tag: string): void

  /**
   * Marks loading as completed, hiding it again and flashing a success text
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @memberof ISnackbar
   */
  finishedLoading(tag: string): void
}

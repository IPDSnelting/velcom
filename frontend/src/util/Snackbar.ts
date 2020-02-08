export declare interface ISnackbar {
  /**
   * Displays an error message in the snackbar.
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {string} error the error message
   * @param {number} priority the priority of the message. Higher priorities overwrite lower ones.
   * @memberof ISnackbar
   */
  setError(tag: string, error: string, priority?: number): void

  /**
   * Displays a success message in the snackbar
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {string} message the message
   * @param {number} priority the priority of the message. Higher priorities overwrite lower ones.
   * @memberof ISnackbar
   */
  setSuccess(tag: string, message: string, priority?: number): void

  /**
   * Displays a loading indicator in the snack bar.
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {number} priority the priority of the message. Higher priorities overwrite lower ones.
   * @memberof ISnackbar
   */
  setLoading(tag: string, priority?: number): void

  /**
   * Marks loading as completed, hiding it again and flashing a success text.
   *
   * @param {string} tag a small tag to mark the message (e.g. "Queue" is loading)
   * @param {number} priority the priority of the message. Higher priorities overwrite lower ones.
   * @memberof ISnackbar
   */
  finishedLoading(tag: string, priority?: number): void
}

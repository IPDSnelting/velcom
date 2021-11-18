# Specification for the benchmark repo and benchmark script

## Warning

The JSON output format has been changed slightly compared to the old specification.
In particular, the names of the keys for single measurements have been changed:
* `"results"` → `"values"`
* `"resultInterpretation"` → `"interpretation"`

Also, the unit and interpretation are now required even if a measurement failed.

## Benchmark repo

The benchmark repo is a git repository.
It contains an executable called `bench` in its root directory.
This executable is called the benchmark script.
The benchmark repo may contain additional files and directories.

## Benchmark script

### Executing the script
When a benchmark run is performed, the benchmark script is run.
The working directory in which the benchmark script is run is unspecified.

Its only argument is a path to the git repository it is supposed to test.
This path is either absolute or it is relative to the working directory in which the script is executed.

Example:
```bash
./bench ../my_little_compiler/
```

| Return code | Explanation            |
|-------------|------------------------|
| 0           | No error               |
| 1           | Internal script error  |
| 2           | Incorrect script usage |

The script should output its results on `stdout`.
The result is encoded in JSON.
Output to `stderr` is shown only while benchmarking (streamed to the
task-detail page) and in case of a *benchmark script panic* (see below).

### Failure modes

Executing the `bench` script isn't always successful and VelCom groups the
errors into two categories:
- `bench` script panics, which indicate that the benchmark script itself
  crashed (i.e. the author of the benchmark repo needs to fix a bug).  
  Example: The `bench` script suffered a segmentation fault.
- errors of the benchmarked program, which indicate that the `bench` script
  worked correctly, but the revision it tested had a critical problem (i.e. the
  program under test has a problem and needs to be fixed).  
  Example: The program under test fails to compile and therefore the bench
  `script` can not test it.

Benchmark script panics are indicated by a non-zero exit code of the `bench`
script. If this happens, VelCom tries to provide as much information as it can
in the UI, including the full `stdout`, `stderr`, information about the
execution environment and maybe a potential cause for the error. Panics always
take precedence over errors of the benchmarked program: If the `bench` script
exits with a non-zero exit code, it is *always* treated as a benchmark script
panic.


If instead the program under test suffered a critical failure and the `bench`
script is not able to test it, it should use the following output format and
terminate with a zero exit code:
```
{
    "error": <error_string>
}
```
* `<error_string>` is a string describing the error.


## Output format

If the `bench` script did not crash and the tested revision works well enough
that measurements could be taken, this format should be used:
```
{
    <benchmark>: {
        <metric>: <measurements>
    }
}
```
* `<benchmark>` and `<metric>` are the benchmark and metric names (as strings) for which the measurements were taken.
* `<measurements>` is described by the following format.

If the measurement was taken successfully, this format should be used for `<measurements>`:
```
{
    "unit": <unit>,
    "interpretation": <interpretation>,
    "values": <values>
}
```
* `<unit>` is the name of the unit (as string) the values are in.
* `<interpretation>` can be one of `"LESS_IS_BETTER"`, `"MORE_IS_BETTER"` or `"NEUTRAL"`.
* `<values>` is an array containing the measured values as numbers. The values will be interpreted as 64 bit floating point.

If an error occurred that prevented this set of measurements to be taken, this format should be used for `<measurements>`:
```
{
    "unit": <unit>,
    "interpretation": <interpretation>,
    "error": <error_string>
}
```
* `<unit>` is the name of the unit (as string) the values are in.
* `<interpretation>` can be one of `"LESS_IS_BETTER"`, `"MORE_IS_BETTER"` or `"NEUTRAL"`.
* `<error_string>` is a string describing the error.

### Output format examples

A successful run with two successful sets and one failed set of measurements:
```json
{
    "build": {
        "time": {
            "values": [13.2, 15.12, 12.83, 13.74, 13.58],
            "unit": "seconds",
            "interpretation": "LESS_IS_BETTER"
        },
        "loc": {
            "values": [3038],
            "unit": "lines",
            "interpretation": "NEUTRAL"
        }
    },
    "run": {
        "time": {
            "error": "Program exited with error code 1"
        }
    }
}
```

A failed run:
```json
{
    "error": "Could not find Makefile"
}
```

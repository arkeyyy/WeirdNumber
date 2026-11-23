# Weird Number

Weird Number is a JavaFX desktop application that finds all weird numbers from 1 through an inclusive upper limit.

A number is **weird** when its positive proper divisors add up to more than the number, yet no subset of them adds up to the number exactly. For example, the proper divisors of 70 are 1, 2, 5, 7, 10, 14, and 35. Their sum is 74, and no subset totals 70.

## Search behavior

The default search covers 1 through 10,000 with four worker threads. Each worker checks a contiguous range and has its own progress indicator. The maximum thread count is the smaller of 64 and the upper limit.

A completed search lists the weird numbers in ascending order and shows the counts of weird numbers, other numbers, and all numbers checked. Stopping a search discards its partial results.

| Upper limit | Threads | Weird numbers | Other numbers | Total checked |
| ---: | ---: | ---: | ---: | ---: |
| 16,000 | 4 | 23 | 15,977 | 16,000 |

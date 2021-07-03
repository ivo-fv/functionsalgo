(on hold indefinitely)

![Java CI with Maven](https://github.com/ivo-fv/functionsalgo/workflows/Java%20CI%20with%20Maven/badge.svg?branch=main) ![CodeQL](https://github.com/ivo-fv/functionsalgo/workflows/CodeQL/badge.svg)
# functionsalgo
Automated trading algorithms often involve a backtesting process in their development, which means there must be code that is run for backtesting purposes and the code that will run in production (live). However, these two codes may fail in their attempt to represent the same trading algorithm.

The solution is to just make them one and the same! Meaning that the code representing the trading algorithm for backtesting is the same code used in production.

This project provides a general outline (not a framework) to facilitate implementing a trading algorithm while only needing to write one version of code that can be ran both for backtests and live production.

The live production environment is intended to be serverless functions (e.g. lambda, gcp functions) and not event based, so it's not suitable for sub minute trading frequencies/intervals. Besides this, there are no other constraints (e.g. it's possible to trade between multiple exchanges and use various data sources at the same time).

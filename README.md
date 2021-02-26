![Java CI with Maven](https://github.com/ivo-fv/functionsalgo/workflows/Java%20CI%20with%20Maven/badge.svg?branch=main) ![CodeQL](https://github.com/ivo-fv/functionsalgo/workflows/CodeQL/badge.svg)
# functionsalgo
Automated trading algorithms often involve a backtesting process in their development, which means there must be code that is run for backtesting purposes and the code that will run in production (live). However, these two codes may fail in their attempt to represent the same trading algorithm.

The solution is to just make them one and the same! Meaning that the code representing the trading algorithm for backtesting is the same code used in production.

This project provides a general outline (not a framework) to facilitate implementing a trading algorithm while only needing to write one version of code that can be ran both for backtests and live production, while allowing me to practise the usage of some common development procedures and Java ecosystem tools (e.g. continuous integration, maven, test mocking with mockito, handling configuration files, external web APIs...).

The live production environment is intended to be serverless functions (e.g. lambda, gcp functions) and not event based, so it's not suitable for sub minute trading frequencies/intervals. Besides this, there are no other constraints (e.g. it's possible to trade between multiple exchanges and use various data sources at the same time).

## Usage
The trading program is its own maven module that will implement the interfaces and use the classes in the shared module. One of the interfaces is Strategy, in which the trading algorithm is implemented and invoked via the execute(timestamp) method which will be called once for each execution of the serverless function when in production. 

The strategy module can use various components to interface with external APIs (when ran in production) or simulate market conditions (when backtesting) that can be implemented and packaged separately. The deployable or runnable is packaged as an uber jar of the strategy module.

An example of the intended usage is provided in the sample-strategy module. This sample-strategy is trading cryptocurrency perpetual swaps on the Binance exchange and makes use of the example components in the sample-components module.

## Backtesting
Backtesting can be done either programmatically or through the command line (for more info. see the Backtester javadoc). The statistics shown at the end of a backtest can include the defaults or/and ones custom made specifically for the trading algorithm. In a live environment these statistics could be stored in a database and be used to monitor the strategy's live perfomance.

[sample-strategy backtest example](../main/sample_strat_backtest_example.webm) <sup><sub>(in this example, sample.jar is the uber jar of the sample-strategy module and it shows very basic statistics relating to 'balance' and 'wallet balance' are shown. The former is a general default for all strategies, the latter a custom made one specific to sample-strategy)</sub></sup>

## Todo list
- javadoc 
- implement an example serverless cloud function adapter to make it work live (will be aws lambda, deployment through uploading an uber jar)
- will need a database to store whatever the algorithm may require in between each lambda invocation (probably aws dynamodb)
- a default statistic showing an .svg plot of the overall balance over time
- a ta4j or TA-Lib adapter to make it easy to use common trading indicators
- a front end to provide an overview, monitor and control a strategy when live
- allow python to be used to code a strategy algorithm

## License
All code found in this repository is licensed under the GPL v3 license, see the LICENSE file in the project root for the full license text.

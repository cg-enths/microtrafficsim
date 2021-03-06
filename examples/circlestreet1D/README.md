# Microscopic Traffic Simulation - 1D Single Laned

![Teaser: 1D Nagel-Schreckenberg-Model](../../resources/teaser_2018-06-26_NaSch1D.png "Teaser: 1D Nagel-Schreckenberg-Model")


## Requirements

* `python3`

using additional python packages
* `matplotlib`
* `numpy`


## Usage

You can call the example directly using python or using a Gradle task:
```shell
# using python
py examples/circlestreet1D/src/main.py <args>

# Gradle
./gradlew :examples:circlestreet1D:run -Dexec.args="<args>"
```
For further information, set `<args>` to `-h`.


## What you can see

The x-axis shows a whole 1D-street and its vehicles.
The y-axis shows the timeline.
This means a line at y-value 40 was the current street 40 steps ago.
The colorbar shows the velocity of the vehicles in `cells per second`.
In short, `1 cps = 27 km/h`.
For more information about this, see our section about [the Nagel-Schreckenberg-Model in the wiki](https://github.com/sgs-us/microtrafficsim/wiki/Implementation-Details#nagel-schreckenberg-model).

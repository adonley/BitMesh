Quick tutorial on gulp:

cd .../WebClient
npm install // this installs all the stuff in package.json, which defines the dependencies
npm install -g gulp // you may not need this, but it makes things a little simpler, the -g flag makes gulp globally available

Ok now you have gulp. Look in node_modules to see all the stuff that got installed.

To run gulp, cd to the directory with gulpfile.js in it, then do

gulp [task_name]

where task_name is one of the tasks defined in the gulpfile.js. If no task is specified, it will run the 'default' task.

Gulp is asynchronous! If you specify three arguments for gulp.task, then the middle argument is the name of a task that your task depends on. This is how you make gulp be synchronous if you need it to be.

Other than that, just put whatever javascript you want in the gulp file.

Note: require('blah') is how node.js handles dependencies. Require.js first looks in node_modules for the module, then elsewhere...


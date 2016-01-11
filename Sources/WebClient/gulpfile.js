var gulp = require('gulp');

var jshint = require('gulp-jshint');
var concat = require('gulp-concat');
var uglify = require('gulp-uglify');
var rename = require('gulp-rename');
var minify_html = require('gulp-minify-html');
var minify_css = require('gulp-minify-css');
var strip_css_comments = require('gulp-strip-css-comments');
var strip_debug = require('gulp-strip-debug');
var replace = require('gulp-replace');
var streamqueue = require('streamqueue');
var file_prefix = './';


/**
 * gulp.task defines a task, in this case 'lint', which is called from command line in the directory of the gulpfile
 * the second argument defines the behavior of the defined task
 */
gulp.task('lint', function() {
	return gulp.src('js/*')
		.pipe(jshint())
		.pipe(jshint.reporter('default'));
});

gulp.task('scripts', function() {
	return streamqueue({ objectMode: true },
        gulp.src(file_prefix + 'js/jquery.js'),
        gulp.src(file_prefix + 'js/jquery.qrcode.js'),
        gulp.src(file_prefix + 'js/qrcode.js'),
        gulp.src(file_prefix + 'js/bootstrap.js'),
        gulp.src(file_prefix + 'js/Long.js'),
        gulp.src(file_prefix + 'js/ByteBufferAB.js'),
        gulp.src(file_prefix + 'js/ProtoBuf.js'),
        gulp.src(file_prefix + 'js/bitcore.js'),
        gulp.src(file_prefix + 'js/bitcore-channel.js'),
        gulp.src(file_prefix + 'js/bitmesh.js'))
				.pipe(concat('bitmesh.all.js'))
        .pipe(gulp.dest(file_prefix + 'js'));
});

gulp.task('html', function() {
    gulp.src('bitmesh.html')
        .pipe(rename('bitmesh_mobile.html'))
        .pipe(replace('bitmesh.all.js','bitmesh_mobile.all.js'))
        .pipe(gulp.dest(file_prefix));
});

gulp.task('styles', function() {
	return gulp.src(['css/bootstrap.css',
						  'css/bootstrap-theme.css',
						  'css/bitmesh.css'])
					.pipe(concat('bitmesh.all.css'))
          .pipe(gulp.dest(file_prefix + 'css'));
});

gulp.task('images', function() {
	return gulp.src('images/*')
					.pipe(gulp.dest(file_prefix + 'dist/images'));
});

gulp.task('protos', function() {
	return gulp.src('proto/paymentchannel.proto')
					.pipe(gulp.dest(file_prefix + 'dist/proto'));
});

gulp.task('fonts', function() {
	return gulp.src('fonts/*/*')
					.pipe(gulp.dest(file_prefix + 'dist/fonts/'));
});

gulp.task('mobile', function() {
   return streamqueue({ objectMode: true },
        gulp.src(file_prefix + 'js/jquery.js'),
        gulp.src(file_prefix + 'js/jquery.qrcode.js'),
        gulp.src(file_prefix + 'js/qrcode.js'),
        gulp.src(file_prefix + 'js/bootstrap.js'),
        gulp.src(file_prefix + 'js/Long.js'),
        gulp.src(file_prefix + 'js/ByteBufferAB.js'),
        gulp.src(file_prefix + 'js/ProtoBuf.js'),
        gulp.src(file_prefix + 'js/bitcore.js'),
        gulp.src(file_prefix + 'js/bitmesh_mobile.js')
    )
               .pipe(concat('bitmesh_mobile.all.js'))
               .pipe(gulp.dest(file_prefix + 'js'));
});

gulp.task('watch', function() {
   gulp.watch([file_prefix + 'js/*'], ['scripts', 'mobile']);
   gulp.watch([file_prefix + '*.html'], ['html']);
   gulp.watch([file_prefix + 'css/*'], ['styles', 'fonts']);
   gulp.watch([file_prefix + 'proto/paymentchannel.proto'], ['protos']);
   gulp.watch([file_prefix + 'images/*'], ['images']);
   gulp.watch([file_prefix + 'fonts/*/*'], ['fonts']);
});

// deploy depends on default, so default must run first
gulp.task('deploy', ['default'], function() {
    gulp.src(file_prefix + 'js/bitmesh.all.js')
            .pipe(strip_debug())
            .pipe(rename('bitmesh.all.min.js'))
            .pipe(uglify())
            .pipe(gulp.dest(file_prefix + 'dist/js'));
    gulp.src(file_prefix + 'js/bitmesh_mobile.all.js')
            .pipe(strip_debug())
            .pipe(rename('bitmesh_mobile.all.min.js'))
            .pipe(uglify())
            .pipe(gulp.dest(file_prefix + 'dist/js'));
    gulp.src(file_prefix + 'css/bitmesh.all.css')
            .pipe(rename('bitmesh.all.min.css'))
            .pipe(strip_css_comments())
            .pipe(minify_css())
            .pipe(gulp.dest(file_prefix + 'dist/css'));
    gulp.src('bitmesh.html')
            .pipe(replace('bitmesh.all.css','bitmesh.all.min.css'))
            .pipe(replace('bitmesh.all.js','bitmesh.all.min.js'))
            .pipe(minify_html())
            .pipe(gulp.dest(file_prefix + 'dist'));
    gulp.src('bitmesh.html')
            .pipe(rename('bitmesh_mobile.html'))
            .pipe(replace(/<!--DELETE-->[\s\S]*?<!--DELETE-->/g, ''))
            .pipe(replace('bitmesh.all.js','bitmesh_mobile.all.js'))
            .pipe(gulp.dest(file_prefix))
            .pipe(replace('bitmesh_mobile.all.js','bitmesh_mobile.all.min.js'))
            .pipe(minify_html())
            .pipe(gulp.dest(file_prefix + 'dist'));

});

gulp.task('default', [/*'lint',*/ 'scripts', 'html', 'styles',
							 'images', 'protos', 'fonts', 'mobile']);


gulp.task('templates', function(){
  gulp.src(['file.txt'])
    .pipe(replace('bar', 'foo'))
    .pipe(gulp.dest('build/file.txt'));
});

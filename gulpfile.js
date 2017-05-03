/*jslint browser: true, white: true, node: true*/
/*global $, jQuery, gulp, require, scss*/

var gulp          = require('gulp'),
    gutil         = require('gulp-util'),
    sass          = require('gulp-sass'),
    autoprefixer  = require('gulp-autoprefixer'),
    minifycss     = require('gulp-minify-css'),
    minifyHTML    = require('gulp-minify-html'),
    concat        = require('gulp-concat'),
    stripDebug    = require('gulp-strip-debug'),
    uglify        = require('gulp-uglify'),
    changed       = require('gulp-changed');

//SASS Compile
gulp.task('styles', function () {
    'use strict';
    console.log('styles');
     gulp.src( "./style/style.scss" ).pipe( 
         sass() ).pipe(gulp.dest("./style"));
});

gulp.task('minify-css', ['styles'], function () {
    'use strict';
    console.log('minify-css');

    return gulp.src(['style/libs/*.css', 'style/style.css'])
        .pipe(minifycss({
            compatibility: 'ie8'
        }))
        .pipe(concat('app.css'))
        .pipe(gulp.dest('./dist/'));
});

// JS concat, strip debugging and minify
gulp.task('scripts', function () {
    'use strict';
    return gulp.src([
        'js/libs/*.js',
        'js/lib.js',
        'js/init.js'
        ])
        .pipe(uglify())
        .pipe(concat('app.js'))
        .pipe(gulp.dest('./dist/'));
});

//compress HTML
gulp.task('htmlpage', function () {
    'use strict';
    var htmlSrc = 'html/*.html';
    gulp.src(htmlSrc)
        .pipe(changed(htmlSrc))
        .pipe(minifyHTML())
        .pipe(gulp.dest(''));
});

gulp.task('watch', function() {
    'use strict';
  gulp.watch(['style/libs/*.css', 'style/*.scss', 'style/*.css'], ['minify-css']);
  gulp.watch(['js/*.js'], ['scripts']);
  gulp.watch(['html/*.html'], ['htmlpage']);
});

gulp.task('default', ['watch']);
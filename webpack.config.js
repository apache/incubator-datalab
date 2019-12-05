const path = require('path');
const webpack = require('webpack');
const jquery = require.resolve('jquery');
const ExtractTextWebpackPlugin = require("extract-text-webpack-plugin");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const isDevelopment = process.env.NODE_ENV === 'development';

module.exports = {
    entry: {
        index:'./src/index.js'
    },
    output: {
        filename: 'main.js',
        path: path.resolve(__dirname, 'dist'),
        publicPath:'dist/'
    },
    module: {
        rules: [
            {
                    test: /\.module\.s(a|c)ss$/,
            loader: [
              isDevelopment ? 'style-loader' : MiniCssExtractPlugin.loader,
              {
                loader: 'css-loader',
                options: {
                  modules: true,
                      sourceMap: isDevelopment
                }
          },
          {
            loader: 'sass-loader',
                options: {
                  sourceMap: isDevelopment
                }
          }
        ]
      },
      {
        test: /\.s(a|c)ss$/,
            exclude: /\.module.(s(a|c)ss)$/,
            loader: [
              'style-loader',
              'css-loader',
              {
                loader: 'sass-loader',
                options: {
                  sourceMap: isDevelopment
                }
          }
        ]
      },
            {
                test: /\.(jpe?g|png|gif|svg)$/i,
                loader:"file-loader",
                options:{
                    name:'[name].[ext]',
                    outputPath:'./images'
                }
            }
        ]
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: jquery,
            jQuery: jquery,
            "window.jQuery": "jquery'",
            "window.$": "jquery"
        }),
        new ExtractTextWebpackPlugin("[name].css"),
        new MiniCssExtractPlugin({
                filename: isDevelopment ? '[name].css' : '[name].[hash].css', chunkFilename: isDevelopment ? '[id].css' : '[id].[hash].css'
     })
    ]
};
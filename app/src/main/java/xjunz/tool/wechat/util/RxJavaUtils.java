/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.util;

import androidx.annotation.NonNull;

import org.reactivestreams.Subscription;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import xjunz.tool.wechat.impl.model.account.Account;

/**
 * 方便创建各种{@link io.reactivex.Flowable}的工具类，避免写模板代码
 * <p>
 * 所创建的所有{@link Flowable}都是在{@link Schedulers#newThread()}中订阅，在
 * {@link AndroidSchedulers#mainThread()}中被观察。
 * </p>
 */
public class RxJavaUtils {
    /**
     * 快速创建一个{@link Completable}
     *
     * @param complete 发送{@link CompletableEmitter#onComplete()}前欲执行的{@link Action}
     * @return {@link Completable}
     */
    public static Completable complete(@NonNull Action complete) {
        return Completable.create(emitter -> {
            complete.run();
            emitter.onComplete();
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 快速创建一个{@link Single}
     *
     * @param success {@link io.reactivex.SingleEmitter#onSuccess(T)}传入的{@link Callable<T>}
     * @param <T>     指定泛型
     * @return {@link Single}
     */
    public static <T> Single<T> single(@NonNull Callable<T> success) {
        return Single.create((SingleOnSubscribe<T>) emitter -> emitter.onSuccess(success.call())).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 快速创建一个{@link Maybe}
     *
     * <p>当{@param success}返回的{@link T}为空时，执行{@link MaybeEmitter#onComplete()},
     * 否则执行{@link MaybeEmitter#onSuccess(T)}，类似于可以传空值的{@link Single}</p>
     *
     * @param success {@link io.reactivex.MaybeEmitter#onSuccess(T)}传入的{@link Callable<T>}
     * @param <T>     指定泛型
     * @return {@link Maybe}
     */
    public static <T> Maybe<T> maybe(@NonNull Callable<T> success) {
        return Maybe.create((MaybeOnSubscribe<T>) emitter -> {
            T t = success.call();
            if (t != null) {
                emitter.onSuccess(t);
            } else {
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 创建一个从{@link Iterable}发射数据的{@link Flowable}
     *
     * @param iterable 数据源
     * @param <T>      指定泛型
     * @return {@link Flowable}
     */
    public static <T> Flowable<T> stream(Iterable<T> iterable) {
        return Flowable.fromIterable(iterable).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 创建一个从{@link Account}数组发射数据的{@link Flowable}
     *
     * @param array 数据源
     * @param <T>   指定泛型
     * @return {@link Flowable}
     */
    public static <T> Flowable<T> stream(T[] array) {
        return Flowable.fromArray(array).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * {@link SingleObserver}的适配器
     *
     * @param <T> 指定泛型
     */
    public static class SingleObserverAdapter<T> implements SingleObserver<T> {

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(T o) {

        }

        @Override
        public void onError(Throwable e) {

        }
    }

    /**
     * {@link MaybeObserver}的适配器
     *
     * @param <T> 指定泛型
     */
    public static class MaybeObserverAdapter<T> implements MaybeObserver<T> {

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(T t) {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    }

    /**
     * {@link FlowableSubscriber}的适配器
     *
     * @param <T> 指定泛型
     */
    public static class FlowableSubscriberAdapter<T> implements FlowableSubscriber<T> {

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(T t) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {

        }
    }

    /**
     * {@link CompletableObserver}的适配器
     */
    public static class CompletableObservableAdapter implements CompletableObserver {

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {

        }
    }
}

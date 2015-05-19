package com.hanhuy.android.keepshare

import android.app.AlertDialog
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget._
import com.hanhuy.android.common.AndroidConversions._
import com.hanhuy.android.keepshare.Futures._
import com.hanhuy.android.keepshare.TypedResource._
import com.hanhuy.keepassj._
import rx.android.schedulers.AndroidSchedulers.mainThread
import rx.android.widget.{OnTextChangeEvent, WidgetObservable}
import rx.lang.scala.JavaConversions._
import rx.lang.scala.{Observable, Subject, Subscription}

import scala.collection.JavaConverters._


object GroupEditFragment {
  def edit(group: PwGroup) = {
    val f = new GroupEditFragment
    val b = new Bundle
    b.putString(BrowseActivity.EXTRA_GROUP_ID, group.getUuid.ToHexString)
    f.setArguments(b)
    f
  }
  def create(parent: PwGroup) = {
    val f = new GroupEditFragment
    val b = new Bundle
    f.setArguments(b)
    b.putString(BrowseActivity.EXTRA_GROUP_ID, parent.getUuid.ToHexString)
    b.putBoolean(EntryViewActivity.EXTRA_CREATE, true)
    f
  }
}
class GroupEditFragment extends AuthorizedFragment {
  setRetainInstance(true)
  private var model: GroupEditModel = GroupEditModel.blank
  private var baseModel = Option.empty[GroupEditModel]
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
                            savedInstanceState: Bundle) = {
    val view = inflater.inflate(TR.layout.group_edit, container, false)

    val groupId = Option(getArguments) flatMap(a =>
      Option(a.getString(BrowseActivity.EXTRA_GROUP_ID)))

    val creating = Option(getArguments) map (
      _.getBoolean(EntryViewActivity.EXTRA_CREATE, false)) exists identity

    val fieldlist = view.findView(TR.field_list)
    val newfield = view.findView(TR.new_field_button)
    val group = view.findView(TR.edit_group)
    val title = view.findView(TR.edit_title)
    val username = view.findView(TR.edit_username)
    val password = view.findView(TR.edit_password)
    val url = view.findView(TR.edit_url)
    val notes = view.findView(TR.edit_notes)
    val iconObservable: Subject[Int] = Subject()
    val groupObservable: Observable[PwGroup] = Observable.create { obs =>
      group.onGroupChange(g => obs.onNext(g))
      Subscription(group.onGroupChange(null))
    }
    iconObservable.subscribeOn(mainThread).subscribe { icon =>
      model = model.copy(icon = icon)
      title.icon = icon
    }
    groupObservable.subscribeOn(mainThread).subscribe { g =>
      model = model.copy(group = g.getUuid)
    }
    WidgetObservable.text(title.textfield).subscribe((n: OnTextChangeEvent) => {
      model = model.copy(title = Option(n.text))
    })
    WidgetObservable.text(notes.textfield).subscribe((n: OnTextChangeEvent) => {
      model = model.copy(notes = Option(n.text))
    })

    activity.database map { db =>
      groupId map { id =>
        val uuid = new PwUuid(KeyManager.bytes(id))
        db.getRootGroup.FindGroup(uuid, true)
      }
    } onSuccessMain { case g =>
      g foreach { grp =>
        if (creating) {
          group.group = grp
          view.findView(TR.delete).setVisibility(View.GONE)
        } else {
          if (grp.getParentGroup == null) {
            group.setVisibility(View.GONE)
            title.first = true
            view.findView(TR.delete).setVisibility(View.GONE)
          } else
            group.group = grp.getParentGroup
          if (model == GroupEditModel.blank) {
            iconObservable.onNext(Database.Icons(grp.getIconId.ordinal))
            title.text = grp.getName
            notes.text = grp.getNotes
            baseModel = Some(model)
          }
        }
      }
    }

    title.iconfield.onClick {
      EntryEditFragment.iconPicker(activity, title.iconfield, iconObservable.onNext)
    }
    view
  }
}

object GroupEditModel {
  def blank = GroupEditModel(R.drawable.i00_password,
    None, None, PwUuid.Zero)
}
case class GroupEditModel(icon: Int, title: Option[String],
                          notes: Option[String], group: PwUuid)